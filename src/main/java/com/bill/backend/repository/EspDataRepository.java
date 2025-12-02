package com.bill.backend.repository;

import com.bill.backend.model.EspData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspDataRepository extends JpaRepository<EspData, Long> {

    // ✅ Get all records for a specific device by MAC
    List<EspData> findByEspMac(String espMac);

    // ✅ Get the latest record for a specific device by MAC
    EspData findTopByEspMacOrderByTimestampDesc(String espMac);

    // ✅ Get the latest record overall (optional, useful for general latest fetch)
    EspData findTopByOrderByTimestampDesc();
}
