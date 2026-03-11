package com.resumeai.resume.mapper;

import com.resumeai.resume.dto.ResumeDto;
import com.resumeai.resume.entity.Resume;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResumeMapper {
    ResumeDto toDto(Resume resume);
}
