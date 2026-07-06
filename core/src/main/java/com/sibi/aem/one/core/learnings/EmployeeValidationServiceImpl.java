package com.sibi.aem.one.core.learnings;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;

@Component(service = EmployeeValidationService.class)
public class EmployeeValidationServiceImpl implements EmployeeValidationService {
    @Override
    public boolean validateEmployee(EmployeeInfo employeeInfo) {
        return StringUtils.isNotBlank(employeeInfo.getName()) && StringUtils.isNotBlank(employeeInfo.getDepartment()) && StringUtils.isNotBlank(employeeInfo.getEmployeeId());
    }
}
