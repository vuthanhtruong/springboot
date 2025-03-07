package com.example.demo.POST;

import com.example.demo.OOP.Admin;
import com.example.demo.OOP.Employees;
import com.example.demo.OOP.Students;
import com.example.demo.OOP.Teachers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class AdminPost {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @PostMapping("/CapNhatAdmin")
    public String capNhatAdmin(@RequestParam String id,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam String phoneNumber,
                               HttpSession session) {
        // Lấy thông tin AdminID từ Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName(); // Lấy AdminID từ SecurityContext
        Admin admin = entityManager.find(Admin.class, adminId);

        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setEmail(email);
        admin.setPhoneNumber(phoneNumber);
        entityManager.merge(admin);

        return "redirect:/TrangChuAdmin";
    }

    @PostMapping("/ThemGiaoVien")
    public String ThemGiaoVien(
            @RequestParam("EmployeeID") String employeeID,
            @RequestParam("TeacherID") String teacherID,
            @RequestParam("FirstName") String firstName,
            @RequestParam("LastName") String lastName,
            @RequestParam("Email") String email,
            @RequestParam("PhoneNumber") String phoneNumber,
            @RequestParam(value = "MisID", required = false) String misID,
            @RequestParam("Password") String password,
            Model model,
            HttpSession session) {

        // Kiểm tra đăng nhập Admin
        Object adminIDObj = session.getAttribute("AdminID");
        if (adminIDObj == null) {
            model.addAttribute("error", "Bạn cần đăng nhập để thực hiện thao tác này.");
            return "DangNhapAdmin";
        }

        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = admins.get(0);

        // Kiểm tra Employee có tồn tại không
        Employees employee = entityManager.find(Employees.class, employeeID);
        if (employee == null) {
            model.addAttribute("error", "Nhân viên không tồn tại.");
            return "ThemGiaoVien";
        }

        // Kiểm tra trùng lặp
        Teachers existingTeacher = entityManager.find(Teachers.class, teacherID);
        if (existingTeacher != null) {
            model.addAttribute("error", "Mã giáo viên đã tồn tại.");
            return "ThemGiaoVien";
        }

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

        if (emailExists) {
            model.addAttribute("error", "Email này đã được sử dụng.");
            return "ThemGiaoVien";
        }
        if (phoneExists) {
            model.addAttribute("error", "Số điện thoại này đã được sử dụng.");
            return "ThemGiaoVien";
        }
        if (misIDExists) {
            model.addAttribute("error", "MIS ID đã tồn tại.");
            return "ThemGiaoVien";
        }

        // Tạo giáo viên mới
        Teachers giaoVien = new Teachers();
        giaoVien.setEmployee(employee);
        giaoVien.setAdmin(admin);
        giaoVien.setId(teacherID);
        giaoVien.setFirstName(firstName);
        giaoVien.setLastName(lastName);
        giaoVien.setEmail(email);
        giaoVien.setPhoneNumber(phoneNumber);
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password); // Không mã hóa mật khẩu

        entityManager.persist(giaoVien);

        return "redirect:/DanhSachGiaoVien?success=added";
    }

    @PostMapping("/ThemHocSinh")
    public String ThemHocSinh(@RequestParam("EmployeeID") String employeeID,
                              @RequestParam("StudentID") String studentID,
                              @RequestParam("FirstName") String firstName,
                              @RequestParam("LastName") String lastName,
                              @RequestParam("Email") String email,
                              @RequestParam("PhoneNumber") String phoneNumber,
                              @RequestParam(value = "MisID", required = false) String misID,
                              @RequestParam("Password") String password,
                              HttpSession session,
                              Model model) {

        // Kiểm tra đăng nhập Admin
        Object adminIDObj = session.getAttribute("AdminID");
        if (adminIDObj == null) {
            model.addAttribute("error", "Bạn chưa đăng nhập!");
            return "ThemHocSinh";
        }

        Admin admin = entityManager.createQuery("from Admin", Admin.class).getResultList().get(0);

        // Kiểm tra Employee
        Employees employee = entityManager.find(Employees.class, employeeID);
        if (employee == null) {
            model.addAttribute("error", "Nhân viên không tồn tại!");
            return "ThemHocSinh";
        }

        // Kiểm tra trùng lặp
        if (entityManager.find(Students.class, studentID) != null) {
            model.addAttribute("error", "ID học sinh đã tồn tại!");
            return "ThemHocSinh";
        }
        if (!entityManager.createQuery("SELECT s FROM Students s WHERE s.email = :email", Students.class)
                .setParameter("email", email)
                .getResultList().isEmpty()) {
            model.addAttribute("error", "Email này đã được sử dụng!");
            return "ThemHocSinh";
        }
        if (!entityManager.createQuery("SELECT s FROM Students s WHERE s.phoneNumber = :phoneNumber", Students.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList().isEmpty()) {
            model.addAttribute("error", "Số điện thoại này đã được sử dụng!");
            return "ThemHocSinh";
        }
        if (misID != null && !misID.isEmpty() &&
                !entityManager.createQuery("SELECT s FROM Students s WHERE s.misId = :misID", Students.class)
                        .setParameter("misID", misID)
                        .getResultList().isEmpty()) {
            model.addAttribute("error", "MIS ID đã tồn tại!");
            return "ThemHocSinh";
        }

        // Tạo đối tượng học sinh mới
        Students student = new Students();
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
        Admin admin = admins.get(0);  // Lấy trực tiếp đối tượng Admin từ danh sách


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
                             @RequestParam(value = "employeeID", required = false) String employeeID,
                             RedirectAttributes redirectAttributes) {

        // Tìm học sinh theo ID
        Students student = entityManager.find(Students.class, id);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy học sinh với ID: " + id);
            return "redirect:/DanhSachHocSinh";
        }

        // Kiểm tra email trùng lặp với học sinh khác
        boolean emailExists = !entityManager.createQuery(
                        "SELECT s FROM Students s WHERE s.email = :email AND s.id <> :id", Students.class)
                .setParameter("email", email)
                .setParameter("id", id)
                .getResultList().isEmpty();

        if (emailExists) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng!");
            return "redirect:/SuaHocSinh/" + id;
        }

        // Kiểm tra số điện thoại trùng lặp với học sinh khác
        boolean phoneExists = !entityManager.createQuery(
                        "SELECT s FROM Students s WHERE s.phoneNumber = :phoneNumber AND s.id <> :id", Students.class)
                .setParameter("phoneNumber", phoneNumber)
                .setParameter("id", id)
                .getResultList().isEmpty();

        if (phoneExists) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại này đã được sử dụng!");
            return "redirect:/SuaHocSinh/" + id;
        }

        // Kiểm tra MIS ID trùng lặp (chỉ kiểm tra nếu MIS ID không rỗng)
        if (misId != null && !misId.isEmpty()) {
            boolean misIDExists = !entityManager.createQuery(
                            "SELECT s FROM Students s WHERE s.misId = :misId AND s.id <> :id", Students.class)
                    .setParameter("misId", misId)
                    .setParameter("id", id)
                    .getResultList().isEmpty();

            if (misIDExists) {
                redirectAttributes.addFlashAttribute("error", "MIS ID đã tồn tại!");
                return "redirect:/SuaHocSinh/" + id;
            }
        }

        // Cập nhật thông tin học sinh
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setMisId(misId);

        // Cập nhật nhân viên phụ trách nếu có
        if (employeeID != null && !employeeID.isEmpty()) {
            Employees employee = entityManager.find(Employees.class, employeeID);
            student.setEmployee(employee);
        } else {
            student.setEmployee(null); // Cho phép bỏ chọn nhân viên
        }

        // Lưu thay đổi vào database
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
                              @RequestParam(value = "employeeID", required = false) String employeeID,
                              RedirectAttributes redirectAttributes) {

        // Tìm giáo viên theo ID
        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy giáo viên với ID: " + id);
            return "redirect:/DanhSachGiaoVien";
        }

        // Kiểm tra trùng email (trừ giáo viên hiện tại)
        TypedQuery<Teachers> emailQuery = entityManager.createQuery(
                "SELECT t FROM Teachers t WHERE t.email = :email AND t.id <> :id", Teachers.class);
        emailQuery.setParameter("email", email);
        emailQuery.setParameter("id", id);
        if (!emailQuery.getResultList().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại!");
            return "redirect:/SuaGiaoVien/" + id;
        }

        // Kiểm tra trùng số điện thoại (trừ giáo viên hiện tại)
        TypedQuery<Teachers> phoneQuery = entityManager.createQuery(
                "SELECT t FROM Teachers t WHERE t.phoneNumber = :phoneNumber AND t.id <> :id", Teachers.class);
        phoneQuery.setParameter("phoneNumber", phoneNumber);
        phoneQuery.setParameter("id", id);
        if (!phoneQuery.getResultList().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại đã tồn tại!");
            return "redirect:/SuaGiaoVien/" + id;
        }

        // Kiểm tra trùng MIS ID nếu có nhập
        if (misID != null && !misID.isEmpty()) {
            TypedQuery<Teachers> misQuery = entityManager.createQuery(
                    "SELECT t FROM Teachers t WHERE t.misID = :misID AND t.id <> :id", Teachers.class);
            misQuery.setParameter("misID", misID);
            misQuery.setParameter("id", id);
            if (!misQuery.getResultList().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "MIS ID đã tồn tại!");
                return "redirect:/SuaGiaoVien/" + id;
            }
            teacher.setMisID(misID);
        }

        // Cập nhật thông tin giáo viên
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(email);
        teacher.setPhoneNumber(phoneNumber);

        // Cập nhật nhân viên phụ trách nếu có
        if (employeeID != null && !employeeID.isEmpty()) {
            Employees employee = entityManager.find(Employees.class, employeeID);
            if (employee != null) {
                teacher.setEmployee(employee);
            } else {
                redirectAttributes.addFlashAttribute("error", "Nhân viên không tồn tại!");
                return "redirect:/SuaGiaoVien/" + id;
            }
        } else {
            teacher.setEmployee(null);
        }

        // Lưu thay đổi
        entityManager.merge(teacher);
        redirectAttributes.addFlashAttribute("success", "Cập nhật thành công!");

        return "redirect:/DanhSachGiaoVien";
    }

    @PostMapping("/SuaNhanVien/{id}")
    public String CapNhatNhanVien(@PathVariable("id") String id,
                                  @ModelAttribute Employees updatedEmployee,
                                  RedirectAttributes redirectAttributes) {

        Employees existingEmployee = entityManager.find(Employees.class, id);
        if (existingEmployee == null) {
            redirectAttributes.addFlashAttribute("error", "Nhân viên không tồn tại!");
            return "redirect:/DanhSachNhanVien";
        }

        // Kiểm tra trùng Email
        TypedQuery<Employees> emailQuery = entityManager.createQuery(
                "SELECT e FROM Employees e WHERE e.email = :email AND e.id <> :id", Employees.class);
        emailQuery.setParameter("email", updatedEmployee.getEmail());
        emailQuery.setParameter("id", id);
        List<Employees> emailList = emailQuery.getResultList();
        if (!emailList.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại!");
            return "redirect:/SuaNhanVien/" + id;
        }

        // Kiểm tra trùng số điện thoại
        TypedQuery<Employees> phoneQuery = entityManager.createQuery(
                "SELECT e FROM Employees e WHERE e.phoneNumber = :phone AND e.id <> :id", Employees.class);
        phoneQuery.setParameter("phone", updatedEmployee.getPhoneNumber());
        phoneQuery.setParameter("id", id);
        List<Employees> phoneList = phoneQuery.getResultList();
        if (!phoneList.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại đã tồn tại!");
            return "redirect:/SuaNhanVien/" + id;
        }

        // Cập nhật thông tin nhân viên
        existingEmployee.setFirstName(updatedEmployee.getFirstName());
        existingEmployee.setLastName(updatedEmployee.getLastName());
        existingEmployee.setEmail(updatedEmployee.getEmail());
        existingEmployee.setPhoneNumber(updatedEmployee.getPhoneNumber());

        entityManager.merge(existingEmployee);

        redirectAttributes.addFlashAttribute("success", "Cập nhật nhân viên thành công!");
        return "redirect:/DanhSachNhanVien";
    }

    @PostMapping("/TimKiemHocSinh")
    public String TimKiemHocSinh(@RequestParam("searchType") String searchType, @RequestParam("keyword") String keyword
            , ModelMap ModelMap) {
        if (searchType.equalsIgnoreCase("name")) {
            List<Students> searchResults = entityManager.createQuery(
                            "SELECT s FROM Students s " +
                                    "WHERE LOWER(s.firstName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(s.lastName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(:keyword)", Students.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
            ModelMap.addAttribute("searchResults", searchResults);
        } else if (searchType.equalsIgnoreCase("id")) {
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