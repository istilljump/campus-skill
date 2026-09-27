package com.campus.runner.service;

import com.campus.runner.dto.EmployeeDTO;
import com.campus.runner.dto.EmployeeLoginDTO;
import com.campus.runner.dto.EmployeePageQueryDTO;
import com.campus.runner.entity.Employee;
import com.campus.runner.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    boolean save(EmployeeDTO employeeDTO);

    PageResult<Employee> listEmployee(EmployeePageQueryDTO employeePageQueryDTO);

    void changeEmployeeStatus(Integer status, Long id);


    Employee getEmployeeById(Long id);

    void updateEmployee(EmployeeDTO employeeDTO);
}
