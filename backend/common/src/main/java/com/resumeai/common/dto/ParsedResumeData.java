package com.resumeai.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedResumeData {
    private String name;
    private Contact contact;
    private String summary;
    private List<Experience> experience;
    private List<Education> education;
    private List<String> skills;                   // legacy flat list (fallback)
    private List<SkillCategory> skillCategories;   // preferred: categorized
    private List<Project> projects;
    private List<String> certifications;

    @Data
    public static class Contact {
        private String email;
        private String phone;
        private String location;
        private String linkedin;
        private String github;
        private String website;
    }

    @Data
    public static class Experience {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String location;
        private List<String> bullets;
    }

    @Data
    public static class Education {
        private String institution;
        private String degree;
        private String field;
        private String graduationDate;
        private String gpa;
    }

    @Data
    public static class SkillCategory {
        private String category;
        private List<String> skills;
    }

    @Data
    public static class Project {
        private String name;
        private String description;
        private List<String> technologies;
        private String url;
        private List<String> bullets;
    }
}
