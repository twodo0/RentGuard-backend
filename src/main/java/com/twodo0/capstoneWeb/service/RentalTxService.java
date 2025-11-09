package com.twodo0.capstoneWeb.service;

import com.twodo0.capstoneWeb.domain.RentalSession;
import com.twodo0.capstoneWeb.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalTxService {
    private final RentalRepository rentalRepository;

    @Transactional
    public RentalSession saveStart(RentalSession rentalSession) {
        return rentalRepository.save(rentalSession);
    }
    @Transactional
    public RentalSession saveEnd(RentalSession rentalSession) {
        return rentalRepository.save(rentalSession);
    }
}
