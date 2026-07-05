package com.sibi.aem.one.core.learnings;

import java.util.List;
import java.util.Map;

public interface EmployeePageService {

    String getEmployeeTitle(String path);

    List<String> getChildPageTitles(String parentPath);

    Map<String,String> getEmployeeDepartments();

}