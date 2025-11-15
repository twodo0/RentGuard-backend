package com.twodo0.capstoneWeb.rental.controller;

import com.twodo0.capstoneWeb.rental.domain.enums.Phase;
import com.twodo0.capstoneWeb.rental.dto.RentalDetailDto;
import com.twodo0.capstoneWeb.rental.dto.RentalRowView;
import com.twodo0.capstoneWeb.rental.service.RentalQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rentals")
public class RentalQueryController {

    private final RentalQueryService rentalQueryService;

    @GetMapping("/recent")
    public Page<RentalRowView> recent( @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                                          Pageable pageable
    ){
        return rentalQueryService.recent(pageable);
    }

    @GetMapping("/{rentalId}")
    public RentalDetailDto detail(@PathVariable Long rentalId,
                                  @RequestParam(required = false) Phase phase
                                  ){
        return rentalQueryService.detail(rentalId, phase);
    }

}
