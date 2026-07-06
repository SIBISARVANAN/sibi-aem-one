package com.sibi.aem.one.core.learnings;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = EmployeeConfigService.class)

@Designate(
        ocd = EmployeeConfiguration.class)

public class EmployeeConfigServiceImpl
        implements EmployeeConfigService {

    private String companyName;

    private int maxEmployees;

    private String country;

    @Activate
    protected void activate(EmployeeConfiguration config) {

        companyName = config.companyName();

        maxEmployees = config.maxEmployees();

        country = config.country();
    }

    @Override
    public String getCompanyName() {
        return companyName;
    }

    @Override
    public int getMaxEmployees() {
        return maxEmployees;
    }

    @Override
    public String getCountry() {
        return country;
    }

}