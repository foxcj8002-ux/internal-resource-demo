package com.zerotrust.internalresource.config;

import com.zerotrust.internalresource.entity.DeviceResourceEntity;
import com.zerotrust.internalresource.entity.FileResourceEntity;
import com.zerotrust.internalresource.repository.DeviceResourceRepository;
import com.zerotrust.internalresource.repository.FileResourceRepository;
import com.zerotrust.internalresource.service.DeviceResourceService;
import com.zerotrust.internalresource.service.FileResourceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initializeData(FileResourceRepository files, DeviceResourceRepository devices) {
        return args -> {
            if (files.count() == 0) {
                for (int index = 1; index <= 5; index++) {
                    FileResourceEntity entity = new FileResourceEntity();
                    entity.setResourceId(FileResourceService.RESOURCE_ID);
                    entity.setName("Demo file " + index);
                    entity.setDescription("Fixed internal resource demo file " + index);
                    entity.setCategory(index % 2 == 0 ? "configuration" : "report");
                    entity.setOwner("owner-" + index);
                    entity.setDepartment(index % 2 == 0 ? "operations" : "security");
                    entity.setClassificationLevel(index % 2 == 0 ? "CONFIDENTIAL" : "INTERNAL");
                    entity.setStatus("ACTIVE");
                    entity.setCreatedAt(Instant.now());
                    entity.setUpdatedAt(Instant.now());
                    files.save(entity);
                }
            }
            if (devices.count() == 0) {
                for (int index = 1; index <= 5; index++) {
                    DeviceResourceEntity entity = new DeviceResourceEntity();
                    entity.setResourceId(DeviceResourceService.RESOURCE_ID);
                    entity.setDeviceName("Demo device " + index);
                    entity.setIpAddress("192.168.10." + (10 + index));
                    entity.setDeviceType("simulator");
                    entity.setLocation("lab-" + index);
                    entity.setDepartment(index % 2 == 0 ? "operations" : "security");
                    entity.setSecurityLevel(index % 2 == 0 ? "HIGH" : "MEDIUM");
                    entity.setStatus("ONLINE");
                    entity.setLastHeartbeat(Instant.now());
                    entity.setCreatedAt(Instant.now());
                    entity.setUpdatedAt(Instant.now());
                    devices.save(entity);
                }
            }
        };
    }
}
