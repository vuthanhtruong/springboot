package com.example.demo.POST;

import com.example.demo.OOP.Admin;
import com.example.demo.OOP.Employees;
import com.example.demo.OOP.Students;
import com.example.demo.OOP.Teachers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class AdminPost {
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/DangNhapAdmin")
    public String DangNhapAdmin(@RequestParam("AdminID") String AdminID,
                                @RequestParam("PasswordAdmin") String PasswordAdmin,
                                HttpSession session) {
        Admin admin = entityManager.find(Admin.class, AdminID);

        if (admin == null) {
            return "redirect:/DangNhapAdmin";
        }

        if (!PasswordAdmin.equals(admin.getPassword())) {
            return "redirect:/DangNhapAdmin";
        }

        session.setAttribute("AdminID", AdminID);
        return "redirect:/TrangChuAdmin";
    }
    @PostMapping("/ThemGiaoVien")
    public String ThemGiaoVien(@RequestParam("EmployeeID") String employeeID,
                               @RequestParam("TeacherID") String teacherID,
                               @RequestParam("FirstName") String firstName,
                               @RequestParam("LastName") String lastName,
                               @RequestParam("Email") String email,
                               @RequestParam("PhoneNumber") String phoneNumber,
                               @RequestParam(value = "MisID", required = false) String misID,
                               @RequestParam("Password") String password, HttpSession session) {

        // Lấy thông tin Admin từ session
        Object adminIDObj = session.getAttribute("AdminID");
        if (adminIDObj == null) {
            return "redirect:/DangNhapAdmin?error=notLoggedIn";
        }
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = entityManager.find(Admin.class, admins.get(0));

        // Lấy thông tin Employee
        Employees employee = entityManager.find(Employees.class, employeeID);
        if (employee == null) {
            return "redirect:/DangKyGiaoVien?error=employeeNotFound";
        }

        // Kiểm tra trùng lặp
        boolean teacherExists = entityManager.find(Teachers.class, teacherID) != null;
        boolean emailExists = !entityManager.createQuery("SELECT t FROM Teachers t WHERE t.email = :email", Teachers.class)
                .setParameter("email", email)
                .getResultList().isEmpty();
        boolean phoneExists = !entityManager.createQuery("SELECT t FROM Teachers t WHERE t.phoneNumber = :phoneNumber", Teachers.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList().isEmpty();
        boolean misIDExists = misID != null && !misID.isEmpty() &&
                !entityManager.createQuery("SELECT t FROM Teachers t WHERE t.misID = :misID", Teachers.class)
                        .setParameter("misID", misID)
                        .getResultList().isEmpty();

        // Nếu có trùng lặp, trả về lỗi tương ứng
        if (teacherExists) {
            return "redirect:/ThemGiaoVien?error=teacherIDExists";
        }
        if (emailExists) {
            return "redirect:/ThemGiaoVien?error=emailExists";
        }
        if (phoneExists) {
            return "redirect:/ThemGiaoVien?error=phoneExists";
        }
        if (misIDExists) {
            return "redirect:/DangKyGiaoVien?error=misIDExists";
        }

        // Tạo đối tượng giáo viên mới
        Teachers giaoVien = new Teachers();
        giaoVien.setEmployee(employee);
        giaoVien.setAdmin(admin);
        giaoVien.setId(teacherID);
        giaoVien.setFirstName(firstName);
        giaoVien.setLastName(lastName);
        giaoVien.setEmail(email);
        giaoVien.setPhoneNumber(phoneNumber);
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password);

        // Lưu vào database
        entityManager.persist(giaoVien);

        return "redirect:/DanhSachGiaoVien";
    }

    @PostMapping("/ThemHocSinh")
    public String ThemHocSinh(@RequestParam("EmployeeID") String employeeID,
                              @RequestParam("StudentID") String studentID,
                              @RequestParam("FirstName") String firstName,
                              @RequestParam("LastName") String lastName,
                              @RequestParam("Email") String email,
                              @RequestParam("PhoneNumber") String phoneNumber,
                              @RequestParam(value = "MisID", required = false) String misID,
                              @RequestParam("Password") String password, HttpSession session) {

        // Lấy thông tin Admin từ session
        Object adminIDObj = session.getAttribute("AdminID");
        if (adminIDObj == null) {
            return "redirect:/DangNhapAdmin?error=notLoggedIn";
        }
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = entityManager.find(Admin.class, admins.get(0));


        // Lấy thông tin Employee
        Employees employee = entityManager.find(Employees.class, employeeID);

        // Kiểm tra trùng lặp
        boolean studentExists = entityManager.find(Students.class, studentID) != null;
        boolean emailExists = !entityManager.createQuery("SELECT s FROM Students s WHERE s.email = :email", Students.class)
                .setParameter("email", email)
                .getResultList().isEmpty();
        boolean phoneExists = !entityManager.createQuery("SELECT s FROM Students s WHERE s.phoneNumber = :phoneNumber", Students.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList().isEmpty();
        boolean misIDExists = misID != null && !misID.isEmpty() &&
                !entityManager.createQuery("SELECT s FROM Students s WHERE s.misId = :misID", Students.class)
                        .setParameter("misID", misID)
                        .getResultList().isEmpty();

        // Nếu có trùng lặp, trả về lỗi tương ứng
        if (studentExists) {
            return "redirect:/ThemHocSinh?error=studentIDExists";
        }
        if (emailExists) {
            return "redirect:/ThemHocSinh?error=emailExists";
        }
        if (phoneExists) {
            return "redirect:/ThemHocSinh?error=phoneExists";
        }
        if (misIDExists) {
            return "redirect:/ThemHocSinh?error=misIDExists";
        }

        // Tạo đối tượng học sinh mới
        Students student=new Students();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setPassword(password);
        student.setId(studentID);
        student.setAdmin(admin);
        student.setEmployee(employee);
        student.setMisId(misID);

        // Lưu vào database
        entityManager.persist(student);

        return "redirect:/DanhSachHocSinh";
    }
    @PostMapping("/ThemNhanVien")
    public String ThemNhanVien(@RequestParam String EmployeeID,
                               @RequestParam String FirstName,
                               @RequestParam String LastName,
                               @RequestParam String Email,
                               @RequestParam String PhoneNumber,
                               @RequestParam String Password,
                               HttpSession session) {

        // Kiểm tra đăng nhập
        if (session.getAttribute("AdminID") == null) {
            return "redirect:/ThemNhanVien?error=notLoggedIn";
        }
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = entityManager.find(Admin.class, admins.get(0));


        Employees existingEmployee = entityManager.find(Employees.class, EmployeeID);
        if (existingEmployee != null) {
            return "redirect:/ThemNhanVien?error=employeeIDExists";
        }

        TypedQuery<Employees> emailQuery = entityManager.createQuery(
                "SELECT e FROM Employees e WHERE e.email = :email", Employees.class);
        emailQuery.setParameter("email", Email);
        List<Employees> emailList = emailQuery.getResultList();
        if (!emailList.isEmpty()) {
            return "redirect:/ThemNhanVien?error=emailExists";
        }

        TypedQuery<Employees> phoneQuery = entityManager.createQuery(
                "SELECT e FROM Employees e WHERE e.phoneNumber = :phone", Employees.class);
        phoneQuery.setParameter("phone", PhoneNumber);
        List<Employees> phoneList = phoneQuery.getResultList();
        if (!phoneList.isEmpty()) {
            return "redirect:/ThemNhanVien?error=phoneExists";
        }
        // Tạo nhân viên mới
        Employees employees = new Employees();
        employees.setId(EmployeeID);
        employees.setFirstName(FirstName);
        employees.setLastName(LastName);
        employees.setEmail(Email);
        employees.setPassword(Password);
        employees.setPhoneNumber(PhoneNumber);
        employees.setAdmin(admin);

        entityManager.persist(employees);

        return "redirect:/DanhSachNhanVien";
    }

    @PostMapping("/SuaHocSinh/{id}")
    public String SuaHocSinh(@PathVariable("id") String id,
                             @RequestParam("firstName") String firstName,
                             @RequestParam("lastName") String lastName,
                             @RequestParam("email") String email,
                             @RequestParam("phoneNumber") String phoneNumber,
                             @RequestParam("misId") String misId,
                             @RequestParam(value = "employeeID", required = false) String employeeID) {

        // Tìm học sinh theo ID
        Students student = entityManager.find(Students.class, id);
        if (student == null) {
            throw new EntityNotFoundException("Không tìm thấy học sinh với ID: " + id);
        }

        // Cập nhật thông tin học sinh
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setMisId(misId);

        // Cập nhật nhân viên phụ trách nếu có
        if (employeeID != null) {
            Employees employee = entityManager.find(Employees.class, employeeID);
            student.setEmployee(employee);
        } else {
            student.setEmployee(null); // Cho phép bỏ chọn nhân viên
        }

        // Lưu thay đổi
        entityManager.merge(student);

        return "redirect:/DanhSachHocSinh";
    }
    @PostMapping("/SuaGiaoVien/{id}")
    public String SuaGiaoVien(@PathVariable("id") String id,
                              @RequestParam("firstName") String firstName,
                              @RequestParam("lastName") String lastName,
                              @RequestParam("email") String email,
                              @RequestParam("phoneNumber") String phoneNumber,
                              @RequestParam(value = "misID", required = false) String misID,
                              @RequestParam(value = "employeeID", required = false) String employeeID) {
        // Tìm giáo viên theo ID
        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher == null) {
            return "redirect:/DanhSachGiaoVien"; // Nếu không tìm thấy thì quay về danh sách
        }

        // Cập nhật thông tin giáo viên
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(email);
        teacher.setPhoneNumber(phoneNumber);

        if (misID != null) {
            teacher.setMisID(misID);
        }

        // Nếu có chọn nhân viên phụ trách, cập nhật thông tin nhân viên
        if (employeeID != null) {
            Employees employee = entityManager.find(Employees.class, employeeID);
            teacher.setEmployee(employee);
        } else {
            teacher.setEmployee(null);
        }

        // Lưu thay đổi vào database
        entityManager.merge(teacher);

        return "redirect:/DanhSachGiaoVien";
    }
    @PostMapping("/SuaNhanVien/{id}")
    public String CapNhatNhanVien(@PathVariable("id") String id,
                                  @ModelAttribute Employees updatedEmployee) {
        Employees existingEmployee = entityManager.find(Employees.class, id);
        if (existingEmployee != null) {
            existingEmployee.setFirstName(updatedEmployee.getFirstName());
            existingEmployee.setLastName(updatedEmployee.getLastName());
            existingEmployee.setEmail(updatedEmployee.getEmail());
            existingEmployee.setPhoneNumber(updatedEmployee.getPhoneNumber());

            entityManager.merge(existingEmployee);
        }
        return "redirect:/DanhSachNhanVien";
    }
    @PostMapping("/TimKiemHocSinh")
    public String TimKiemHocSinh(@RequestParam("searchType") String searchType, @RequestParam("keyword") String keyword
    ,ModelMap ModelMap) {
        if(searchType.equalsIgnoreCase("name")){
            List<Students> searchResults = entityManager.createQuery(
                            "SELECT s FROM Students s " +
                                    "WHERE LOWER(s.firstName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(s.lastName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(:keyword)", Students.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
            ModelMap.addAttribute("searchResults", searchResults);
        }
        else if(searchType.equalsIgnoreCase("id")){
            Students students = entityManager.find(Students.class, keyword);
            ModelMap.addAttribute("students", students);
        }
        return "DanhSachTimKiemHocSinh";
    }
    @PostMapping("/TimKiemGiaoVien")
    public String TimKiemGiaoVien(@RequestParam("searchType") String searchType,
                                  @RequestParam("keyword") String keyword,
                                  ModelMap model) {
        List<Teachers> searchResults;

        if (searchType.equalsIgnoreCase("name")) {
            searchResults = entityManager.createQuery(
                            "SELECT t FROM Teachers t " +
                                    "WHERE LOWER(t.firstName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(t.lastName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(CONCAT(t.firstName, ' ', t.lastName)) LIKE LOWER(:keyword)", Teachers.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
        } else if (searchType.equalsIgnoreCase("id")) {
            Teachers teacher = entityManager.find(Teachers.class, keyword);
            searchResults = (teacher != null) ? List.of(teacher) : List.of();
        } else {
            searchResults = List.of();
        }

        model.addAttribute("teachers", searchResults);
        return "DanhSachTimKiemGiaoVien";
    }
    @PostMapping("/TimKiemNhanVien")
    public String TimKiemNhanVien(@RequestParam("searchType") String searchType,
                                  @RequestParam("keyword") String keyword,
                                  ModelMap model) {
        List<Employees> searchResults;

        if (searchType.equalsIgnoreCase("name")) {
            searchResults = entityManager.createQuery(
                            "SELECT e FROM Employees e " +
                                    "WHERE LOWER(e.firstName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(e.lastName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(:keyword)", Employees.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
        } else if (searchType.equalsIgnoreCase("id")) {
            Employees employee = entityManager.find(Employees.class, keyword);
            searchResults = (employee != null) ? List.of(employee) : List.of();
        } else {
            searchResults = List.of();
        }

        model.addAttribute("employees", searchResults);
        return "DanhSachTimKiemNhanVien";
    }



}
