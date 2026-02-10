package com.osc.devops.customer.service;

import com.osc.devops.common.exception.NotFoundException;
import com.osc.devops.customer.dto.ProjectDto;
import com.osc.devops.customer.entity.Customer;
import com.osc.devops.customer.entity.Project;
import com.osc.devops.customer.repository.CustomerRepository;
import com.osc.devops.customer.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;

    public List<ProjectDto.Response> getProjects(Long customerId) {
        return projectRepository.findByCustomerIdAndIsActiveTrueOrderByCreatedAtDesc(customerId)
                .stream().map(ProjectDto.Response::from).collect(Collectors.toList());
    }

    public ProjectDto.Response getProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id));
        return ProjectDto.Response.from(project);
    }

    @Transactional
    public ProjectDto.Response createProject(Long customerId, ProjectDto.CreateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("고객사를 찾을 수 없습니다. id=" + customerId));

        Project project = Project.builder()
                .customer(customer)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .k8sVersion(request.getK8sVersion())
                .namespace(request.getNamespace())
                .domain(request.getDomain())
                .environment(request.getEnvironment())
                .build();
        return ProjectDto.Response.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectDto.Response updateProject(Long id, ProjectDto.UpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id));

        if (request.getName() != null) project.setName(request.getName());
        if (request.getCode() != null) project.setCode(request.getCode());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getK8sVersion() != null) project.setK8sVersion(request.getK8sVersion());
        if (request.getNamespace() != null) project.setNamespace(request.getNamespace());
        if (request.getDomain() != null) project.setDomain(request.getDomain());
        if (request.getEnvironment() != null) project.setEnvironment(request.getEnvironment());
        return ProjectDto.Response.from(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id));
        project.setIsActive(false);
        projectRepository.save(project);
    }

    @Transactional
    public ProjectDto.Response activateProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id));
        project.setIsActive(true);
        return ProjectDto.Response.from(projectRepository.save(project));
    }

    public List<ProjectDto.Response> getAllProjects(Long customerId) {
        return projectRepository.findByCustomerIdOrderByIsActiveDescCreatedAtDesc(customerId)
                .stream().map(ProjectDto.Response::from).collect(Collectors.toList());
    }
}