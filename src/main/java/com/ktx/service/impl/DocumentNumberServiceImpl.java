package com.ktx.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.DocumentSequence;
import com.ktx.repository.DocumentSequenceRepository;
import com.ktx.service.DocumentNumberService;

@Service
public class DocumentNumberServiceImpl implements DocumentNumberService {

    private final DocumentSequenceRepository sequenceRepository;

    public DocumentNumberServiceImpl(DocumentSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Override
    @Transactional
    public String nextContractNo(int year) {
        int nextVal = getNextSequenceValue("CONTRACT_NO", year);
        return String.format("HD-%d-%06d", year, nextVal);
    }

    @Override
    @Transactional
    public String nextInvoiceNo(int year) {
        int nextVal = getNextSequenceValue("INVOICE_NO", year);
        return String.format("INV-%d-%06d", year, nextVal);
    }

    private int getNextSequenceValue(String kind, int year) {
        DocumentSequence sequence = sequenceRepository.findByKindAndYearForUpdate(kind, year)
                .orElseGet(() -> {
                    DocumentSequence newSeq = new DocumentSequence();
                    newSeq.setKind(kind);
                    newSeq.setYear(year);
                    newSeq.setLastValue(0);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        int nextVal = sequence.getLastValue() + 1;
        sequence.setLastValue(nextVal);
        sequenceRepository.save(sequence);
        return nextVal;
    }
}
