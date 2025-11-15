package com.twodo0.capstoneWeb.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;

public final class ImageSizeDetector {
    private static final Logger log = LoggerFactory.getLogger(ImageSizeDetector.class);

    private ImageSizeDetector() {}

    public static Dimension detect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            log.warn("A) Empty bytes");
            return null;
        }
        // 헤더 64바이트 확인(원인 추적용)
        log.debug("HEAD[64]: {}", hexHead(bytes, 64));

        // 0) 컨테이너(ISO-BMFF) 시그니처 우선 체크 (AVIF/HEIC)
        Dimension heif = tryHeifAvif(bytes);
        if (heif != null) {
            log.debug("0) HEIF/AVIF parse -> {}x{}", heif.width, heif.height);
            return heif;
        }

        // A) 일반 ImageIO (TwelveMonkeys 포함)
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage bi = ImageIO.read(bais);
            if (bi != null) {
                Dimension d = new Dimension(bi.getWidth(), bi.getHeight());
                log.debug("A) ImageIO.read -> {}x{}", d.width, d.height);
                return d;
            } else {
                log.debug("A) ImageIO.read returned null");
            }
        } catch (Exception e) {
            log.debug("A) ImageIO.read failed: {}", e.toString());
        }

        // B) JPEG 리더 직접
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
            if (!readers.hasNext()) readers = ImageIO.getImageReadersBySuffix("jpg");
            if (!readers.hasNext()) readers = ImageIO.getImageReadersBySuffix("jpeg");

            while (readers.hasNext()) {
                ImageReader r = readers.next();
                try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                    r.setInput(iis, /*seekForwardOnly*/ false, /*ignoreMetadata*/ true);
                    int w = r.getWidth(0);
                    int h = r.getHeight(0);
                    log.debug("B) {} -> {}x{}", r.getClass().getName(), w, h);
                    return new Dimension(w, h);
                } catch (Exception ex) {
                    log.debug("B) {} failed: {}", r.getClass().getName(), ex.toString());
                } finally {
                    r.dispose();
                }
            }
        } catch (Exception e) {
            log.debug("B) Reader path exception: {}", e.toString());
        }

        // C-1) PNG 직접 파싱
        Dimension png = parsePng(bytes);
        if (png != null) {
            log.debug("C) PNG header parse -> {}x{}", png.width, png.height);
            return png;
        }

        // C-2) JPEG SOF 직접 파싱 (스트림 중간 SOI도 탐지)
        Dimension jpg = parseJpegSOF(bytes);
        if (jpg != null) {
            log.debug("C) JPEG SOF header parse -> {}x{}", jpg.width, jpg.height);
            return jpg;
        }

        if (looksZeroFilled(bytes)) {
            log.warn("C) Bytes look zero-filled near head; upstream read may be corrupted.");
        }

        log.warn("C) Unable to detect image size by all methods");
        return null;
    }

    // =========================================================
    // HEIF/AVIF (ISO-BMFF) 파서: ftyp/meta/iprp/ipco/ispe + ipma 매핑
    // =========================================================
    private static Dimension tryHeifAvif(byte[] data) {
        try {
            // top-level 박스 순회: ftyp 확인 & meta 위치 찾기
            int len = data.length;
            int off = 0;
            boolean isHeifFamily = false;
            int metaStart = -1;
            int metaEnd = -1;

            while (off + 8 <= len) {
                long size = readUInt32(data, off);
                int type = readType(data, off + 4);
                long header = 8;
                if (size == 1) { // largesize
                    if (off + 16 > len) break;
                    size = readUInt64(data, off + 8);
                    header = 16;
                } else if (size == 0) {
                    size = (len - off);
                }
                if (size < header || off + size > len) break;

                int contentStart = (int) (off + header);
                int contentEnd = (int) (off + size);

                if (type == fourCC("ftyp")) {
                    // major_brand + compatible_brands 에서 확인
                    if (contentStart + 8 <= contentEnd) {
                        int major = readType(data, contentStart);
                        // compatible brands
                        Set<Integer> brands = new HashSet<>();
                        brands.add(major);
                        for (int p = contentStart + 8; p + 4 <= contentEnd; p += 4) {
                            brands.add(readType(data, p));
                        }
                        // avif/heic/heif 또는 (mif1|msf1)+heic 계열
                        if (brands.contains(fourCC("avif")) ||
                                brands.contains(fourCC("heic")) ||
                                brands.contains(fourCC("heif")) ||
                                brands.contains(fourCC("mif1")) ||
                                brands.contains(fourCC("msf1"))) {
                            isHeifFamily = true;
                            log.debug("0) ftyp brands={}", brandsToString(brands));
                        }
                    }
                } else if (type == fourCC("meta")) {
                    metaStart = contentStart;
                    metaEnd = contentEnd;
                }

                off += (int) size;
            }

            if (!isHeifFamily) return null;
            if (metaStart < 0) {
                log.debug("0) HEIF family but no meta box");
                return null;
            }

            // meta 는 FullBox(버전/플래그 4바이트) 후에 child
            if (metaStart + 4 > metaEnd) return null;
            int metaChildStart = metaStart + 4;

            // meta 내부에서 pitm(Primary Item), iprp(ipco+ipma) 찾기
            Integer primaryItemId = null;
            int iprpStart = -1;
            int iprpEnd = -1;

            int m = metaChildStart;
            while (m + 8 <= metaEnd) {
                long size = readUInt32(data, m);
                int type = readType(data, m + 4);
                long header = 8;
                if (size == 1) {
                    if (m + 16 > metaEnd) break;
                    size = readUInt64(data, m + 8);
                    header = 16;
                } else if (size == 0) {
                    size = (metaEnd - m);
                }
                if (size < header || m + size > metaEnd) break;

                int cs = (int) (m + header);
                int ce = (int) (m + size);

                if (type == fourCC("pitm")) {
                    // pitm: FullBox. version에 따라 item_ID 크기 다름
                    if (cs + 4 > ce) return null;
                    int version = data[cs] & 0xFF;
                    int p = cs + 4;
                    if (version == 0) {
                        if (p + 2 > ce) return null;
                        primaryItemId = readUInt16(data, p);
                        p += 2;
                    } else {
                        if (p + 4 > ce) return null;
                        primaryItemId = (int) readUInt32(data, p);
                        p += 4;
                    }
                    log.debug("0) pitm primaryItemId={}", primaryItemId);
                } else if (type == fourCC("iprp")) {
                    iprpStart = cs;
                    iprpEnd = ce;
                }

                m += (int) size;
            }

            if (iprpStart < 0) {
                log.debug("0) HEIF meta found but no iprp");
                return null;
            }

            // iprp 내부: ipco(프로퍼티 목록), ipma(매핑) 파싱
            // ipco 내 박스의 인덱스는 1-base
            Map<Integer, Dimension> ispeByIndex = new HashMap<>();
            List<Integer> propertyTypesByIndex = new ArrayList<>(); // 1-based: index-1 위치에 type 저장

            // ipma 항목: itemId -> [property indices]
            Map<Integer, List<Integer>> ipmaMap = new HashMap<>();
            int ipcoStart = -1, ipcoEnd = -1;
            int ipmaStart = -1, ipmaEnd = -1;

            int p = iprpStart;
            while (p + 8 <= iprpEnd) {
                long size = readUInt32(data, p);
                int type = readType(data, p + 4);
                long header = 8;
                if (size == 1) {
                    if (p + 16 > iprpEnd) break;
                    size = readUInt64(data, p + 8);
                    header = 16;
                } else if (size == 0) {
                    size = (iprpEnd - p);
                }
                if (size < header || p + size > iprpEnd) break;

                int cs = (int) (p + header);
                int ce = (int) (p + size);

                if (type == fourCC("ipco")) {
                    ipcoStart = cs; ipcoEnd = ce;
                } else if (type == fourCC("ipma")) {
                    ipmaStart = cs; ipmaEnd = ce;
                }

                p += (int) size;
            }

            // ipco: 속성 박스들 순회하여 ispe를 인덱스와 연결
            if (ipcoStart >= 0) {
                int idx = 1;
                int q = ipcoStart;
                while (q + 8 <= ipcoEnd) {
                    long size = readUInt32(data, q);
                    int type = readType(data, q + 4);
                    long header = 8;
                    if (size == 1) {
                        if (q + 16 > ipcoEnd) break;
                        size = readUInt64(data, q + 8);
                        header = 16;
                    } else if (size == 0) {
                        size = (ipcoEnd - q);
                    }
                    if (size < header || q + size > ipcoEnd) break;

                    int cs = (int) (q + header);
                    int ce = (int) (q + size);

                    propertyTypesByIndex.add(type);

                    if (type == fourCC("ispe")) {
                        // ispe: FullBox(4) + width(4) + height(4)
                        if (cs + 12 <= ce) {
                            int w = (int) readUInt32(data, cs + 4);
                            int h = (int) readUInt32(data, cs + 8);
                            if (w > 0 && h > 0) {
                                ispeByIndex.put(idx, new Dimension(w, h));
                                log.debug("0) ipco[{}]=ispe {}x{}", idx, w, h);
                            }
                        }
                    }
                    // 다른 속성(pixi 등)은 무시

                    q += (int) size;
                    idx++;
                }
            }

            // ipma: itemId -> property indices (필요시 essential bit 제거)
            if (ipmaStart >= 0) {
                int r = ipmaStart;
                if (r + 4 > ipmaEnd) return null;
                int version = data[r] & 0xFF;
                int flags = ((data[r + 1] & 0xFF) << 16) | ((data[r + 2] & 0xFF) << 8) | (data[r + 3] & 0xFF);
                r += 4;
                if (r + 4 > ipmaEnd) return null;
                long entryCount = readUInt32(data, r); r += 4;

                boolean largeIndex = (flags & 1) != 0; // 1이면 16-bit property index
                for (long e = 0; e < entryCount; e++) {
                    int itemId;
                    if (version < 1) {
                        if (r + 2 > ipmaEnd) return null;
                        itemId = readUInt16(data, r); r += 2;
                    } else {
                        if (r + 4 > ipmaEnd) return null;
                        itemId = (int) readUInt32(data, r); r += 4;
                    }
                    if (r + 1 > ipmaEnd) return null;
                    int assocCount = data[r] & 0xFF; r += 1;

                    List<Integer> props = new ArrayList<>();
                    for (int a = 0; a < assocCount; a++) {
                        int rawIndex;
                        if (largeIndex) {
                            if (r + 2 > ipmaEnd) return null;
                            rawIndex = ((data[r] & 0xFF) << 8) | (data[r + 1] & 0xFF);
                            r += 2;
                        } else {
                            if (r + 1 > ipmaEnd) return null;
                            rawIndex = data[r] & 0xFF;
                            r += 1;
                        }
                        // MSB는 essential flag -> 지우고 인덱스만 사용
                        int mask = largeIndex ? 0x7FFF : 0x7F;
                        int propIndex = rawIndex & mask;
                        props.add(propIndex);
                    }
                    ipmaMap.put(itemId, props);
                    if (e < 4) { // 너무 길면 로그 과다이므로 일부만
                        log.debug("0) ipma itemId={} -> {}", itemId, props);
                    }
                }
            }

            if (primaryItemId == null) {
                // 일부 파일은 pitm 없고 단일 이미지인 경우도 있으나, 여기선 pitm 없으면 포기
                log.debug("0) No pitm(primary) in meta");
                return null;
            }

            // primary item의 속성 중 ispe 찾기
            List<Integer> assoc = ipmaMap.get(primaryItemId);
            if (assoc != null) {
                for (Integer idx : assoc) {
                    Dimension d = ispeByIndex.get(idx);
                    if (d != null) return d;
                }
                // ipma에 ispe가 안보이면, ipco의 첫 ispe라도 반환(폴백)
                if (!ispeByIndex.isEmpty()) return ispeByIndex.values().iterator().next();
            } else {
                // ipma가 primary에 대해 없을 때도 ipco의 ispe가 단 하나라면 그걸 사용
                if (ispeByIndex.size() == 1) return ispeByIndex.values().iterator().next();
            }

            log.debug("0) HEIF/AVIF detected but no usable ispe for primary item");
            return null;
        } catch (Exception ex) {
            log.debug("0) HEIF/AVIF parse failed: {}", ex.toString());
            return null;
        }
    }

    // =========================================================
    // PNG
    // =========================================================
    private static Dimension parsePng(byte[] data) {
        // PNG signature
        int sig = indexOf(data, new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        if (sig < 0) return null;
        log.debug("C) PNG signature at offset {}", sig);

        int pos = sig + 8; // 시그니처 뒤
        if (pos + 8 > data.length) return null;

        // 첫 chunk는 IHDR
        int len = readIntBE(data, pos);
        int type = readIntBE(data, pos + 4); // 'IHDR'
        if (type != 0x49484452) {
            log.debug("C) PNG: first chunk is not IHDR (type=0x{})", Integer.toHexString(type));
            return null;
        }
        if (pos + 8 + len > data.length || len < 8) return null;

        int w = readIntBE(data, pos + 8);
        int h = readIntBE(data, pos + 12);
        if (w > 0 && h > 0) return new Dimension(w, h);
        return null;
    }

    // =========================================================
    // JPEG
    // =========================================================
    private static Dimension parseJpegSOF(byte[] data) {
        if (data == null || data.length < 4) return null;

        // SOI(FF D8) 어디서든 검색
        int soi = -1;
        for (int k = 0; k < data.length - 1; k++) {
            if ((data[k] & 0xFF) == 0xFF && (data[k + 1] & 0xFF) == 0xD8) { soi = k; break; }
        }
        if (soi < 0) return null;
        log.debug("C) JPEG SOI at offset {}", soi);

        int i = soi + 2; // SOI 이후
        while (i + 3 < data.length) {
            while (i < data.length && (data[i] & 0xFF) != 0xFF) i++;
            if (i >= data.length - 1) break;
            int ff = i;
            while (i < data.length && (data[i] & 0xFF) == 0xFF) i++;
            if (i >= data.length) break;

            int marker = data[i] & 0xFF;
            if (marker == 0xD8) { // SOI
                log.debug("C) JPEG marker SOI at {}", ff);
                continue;
            }
            if (marker == 0xD9) { // EOI
                log.debug("C) JPEG marker EOI at {}", ff);
                break;
            }
            if (i + 2 >= data.length) return null;

            int len = ((data[i + 1] & 0xFF) << 8) | (data[i + 2] & 0xFF);
            if (len < 2) return null;

            int segStart = i + 3;
            int segEndExclusive = segStart + (len - 2);
            if (segEndExclusive > data.length) return null;

            log.debug("C) JPEG marker 0x{} len={} at {}", Integer.toHexString(marker), len, ff);

            if (isSOF(marker)) {
                if (segStart + 5 > data.length) return null;
                int h = ((data[segStart + 1] & 0xFF) << 8) | (data[segStart + 2] & 0xFF);
                int w = ((data[segStart + 3] & 0xFF) << 8) | (data[segStart + 4] & 0xFF);
                return (w > 0 && h > 0) ? new Dimension(w, h) : null;
            }
            if (marker == 0xDA) { // SOS
                log.debug("C) JPEG reached SOS before SOF; invalid/unsupported stream");
                return null;
            }

            i = segEndExclusive;
        }
        return null;
    }

    private static boolean isSOF(int m) {
        switch (m) {
            case 0xC0: case 0xC1: case 0xC2: case 0xC3:
            case 0xC5: case 0xC6: case 0xC7:
            case 0xC9: case 0xCA: case 0xCB:
            case 0xCD: case 0xCE: case 0xCF:
                return true;
            default:
                return false;
        }
    }

    // =========================================================
    // 유틸
    // =========================================================
    private static String hexHead(byte[] data, int max) {
        int n = Math.min(max, data.length);
        StringBuilder sb = new StringBuilder(n * 3);
        for (int i = 0; i < n; i++) {
            int b = data[i] & 0xFF;
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", b));
        }
        if (data.length > n) sb.append(" ...");
        return sb.toString();
    }

    private static boolean looksZeroFilled(byte[] data) {
        int n = Math.min(64, data.length);
        for (int i = 0; i < n; i++) {
            if ((data[i] & 0xFF) != 0x00) return false;
        }
        return true;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static int readIntBE(byte[] a, int off) {
        if (off + 4 > a.length) return -1;
        return ((a[off] & 0xFF) << 24)
                | ((a[off + 1] & 0xFF) << 16)
                | ((a[off + 2] & 0xFF) << 8)
                |  (a[off + 3] & 0xFF);
    }

    private static long readUInt32(byte[] a, int off) {
        if (off + 4 > a.length) return -1;
        return ((long)(a[off] & 0xFF) << 24)
                | ((long)(a[off + 1] & 0xFF) << 16)
                | ((long)(a[off + 2] & 0xFF) << 8)
                | ((long)(a[off + 3] & 0xFF));
    }

    private static long readUInt64(byte[] a, int off) {
        if (off + 8 > a.length) return -1;
        return ((long)(a[off] & 0xFF) << 56)
                | ((long)(a[off + 1] & 0xFF) << 48)
                | ((long)(a[off + 2] & 0xFF) << 40)
                | ((long)(a[off + 3] & 0xFF) << 32)
                | ((long)(a[off + 4] & 0xFF) << 24)
                | ((long)(a[off + 5] & 0xFF) << 16)
                | ((long)(a[off + 6] & 0xFF) << 8)
                | ((long)(a[off + 7] & 0xFF));
    }

    private static int readUInt16(byte[] a, int off) {
        if (off + 2 > a.length) return -1;
        return ((a[off] & 0xFF) << 8) | (a[off + 1] & 0xFF);
    }

    private static int readType(byte[] a, int off) {
        if (off + 4 > a.length) return 0;
        return ((a[off] & 0xFF) << 24)
                | ((a[off + 1] & 0xFF) << 16)
                | ((a[off + 2] & 0xFF) << 8)
                |  (a[off + 3] & 0xFF);
    }

    private static int fourCC(String s) {
        byte[] b = s.getBytes();
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    private static String brandsToString(Set<Integer> brands) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Integer t : brands) {
            if (!first) sb.append(',');
            first = false;
            sb.append(typeToString(t));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String typeToString(int type) {
        char a = (char)((type >> 24) & 0xFF);
        char b = (char)((type >> 16) & 0xFF);
        char c = (char)((type >> 8) & 0xFF);
        char d = (char)(type & 0xFF);
        return "" + a + b + c + d;
    }
}