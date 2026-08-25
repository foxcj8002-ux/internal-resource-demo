package com.zerotrust.internalresource.service;

import com.zerotrust.internalresource.dto.AccessLogResponse;
import com.zerotrust.internalresource.entity.AccessLogEntity;
import com.zerotrust.internalresource.repository.AccessLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessLogService {
    private final AccessLogRepository repository;

    public AccessLogService(AccessLogRepository repository) {
        this.repository = repository;
    }

    public List<AccessLogResponse> findAll() {
        return repository.findAll().stream().map(AccessLogResponse::from).toList();
    }

    public AccessLogResponse findById(Long id) {
        AccessLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("access log not found: " + id));
        return AccessLogResponse.from(entity);
    }
}
