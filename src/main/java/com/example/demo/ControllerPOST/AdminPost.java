package com.example.demo.ControllerPOST;

import com.example.demo.ModelOOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
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
            RedirectAttributes redirectAttributes) { // Sử dụng RedirectAttributes

        try {
            Admin admin = entityManager.createQuery("FROM Admin", Admin.class).setMaxResults(1).getSingleResult();

            Staffs employee = entityManager.find(Staffs.class, employeeID);
            if (employee == null) {
                redirectAttributes.addFlashAttribute("error", "Nhân viên không tồn tại!");
                return "redirect:/ThemGiaoVien";
            }

            // Kiểm tra trùng TeacherID
            Persons existingTeacher = entityManager.find(Persons.class, teacherID);
            if (existingTeacher != null) {
                redirectAttributes.addFlashAttribute("error", "ID giáo viên đã tồn tại!");
                return "redirect:/ThemGiaoVien";
            }

            // Kiểm tra Email & SĐT
            boolean emailExists = !entityManager.createQuery("SELECT t FROM Persons t WHERE t.email = :email", Persons.class)
                    .setParameter("email", email)
                    .getResultList().isEmpty();

            boolean phoneExists = !entityManager.createQuery("SELECT t FROM Persons t WHERE t.phoneNumber = :phoneNumber", Persons.class)
                    .setParameter("phoneNumber", phoneNumber)
                    .getResultList().isEmpty();

            if (emailExists) {
                redirectAttributes.addFlashAttribute("error", "Email đã tồn tại!");
                return "redirect:/ThemGiaoVien";
            }
            if (phoneExists) {
                redirectAttributes.addFlashAttribute("error", "Số điện thoại đã tồn tại!");
                return "redirect:/ThemGiaoVien";
            }

            // Tạo giáo viên mới
            Teachers newTeacher = new Teachers();
            newTeacher.setEmployee(employee);
            newTeacher.setAdmin(admin);
            newTeacher.setId(teacherID);
            newTeacher.setFirstName(firstName);
            newTeacher.setLastName(lastName);
            newTeacher.setEmail(email);
            newTeacher.setPhoneNumber(phoneNumber);
            newTeacher.setMisID(misID);
            newTeacher.setPassword(password);

            entityManager.persist(newTeacher);

            redirectAttributes.addFlashAttribute("successMessage", "Thêm giáo viên thành công!");
            return "redirect:/DanhSachGiaoVien";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống! Vui lòng thử lại.");
            return "redirect:/ThemGiaoVien";
        }
    }

    // Phương thức định dạng tên (nếu cần)
    private String formatName(String name) {
        // Định dạng tên để có chữ cái đầu viết hoa và các ký tự còn lại viết thường
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
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
                              RedirectAttributes redirectAttributes) {

        // Kiểm tra Admin có tồn tại không
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        if (admins.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy Admin trong hệ thống!");
            return "redirect:/ThemHocSinh";
        }
        Admin admin = admins.get(0);

        // Kiểm tra Employee có tồn tại không
        Staffs employee = entityManager.find(Staffs.class, employeeID);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Nhân viên không tồn tại!");
            return "redirect:/ThemHocSinh";
        }

        // Kiểm tra trùng lặp ID học sinh
        if (entityManager.find(Persons.class, studentID) != null) {
            redirectAttributes.addFlashAttribute("error", "ID học sinh đã tồn tại!");
            return "redirect:/ThemHocSinh";
        }

        // Kiểm tra trùng lặp Email
        boolean emailExists = !entityManager.createQuery("SELECT s FROM Persons s WHERE s.email = :email", Persons.class)
                .setParameter("email", email)
                .getResultList().isEmpty();
        if (emailExists) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng!");
            return "redirect:/ThemHocSinh";
        }

        // Kiểm tra trùng lặp số điện thoại
        boolean phoneExists = !entityManager.createQuery("SELECT s FROM Persons s WHERE s.phoneNumber = :phoneNumber", Persons.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList().isEmpty();
        if (phoneExists) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại này đã được sử dụng!");
            return "redirect:/ThemHocSinh";
        }

        // Kiểm tra trùng lặp MIS ID (nếu có)
        if (misID != null && !misID.isEmpty()) {
            boolean misIdExists = !entityManager.createQuery("SELECT s FROM Students s WHERE s.misId = :misID", Students.class)
                    .setParameter("misID", misID)
                    .getResultList().isEmpty();
            if (misIdExists) {
                redirectAttributes.addFlashAttribute("error", "MIS ID đã tồn tại!");
                return "redirect:/ThemHocSinh";
            }
        }

        // Tạo đối tượng học sinh mới
        Students student = new Students();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setPassword(password); // Mật khẩu có thể cần mã hóa trước khi lưu
        student.setId(studentID);
        student.setAdmin(admin);
        student.setEmployee(employee);
        student.setMisId(misID);

        // Lưu vào database
        entityManager.persist(student);

        // Chuyển hướng đến danh sách học sinh với thông báo thành công
        redirectAttributes.addFlashAttribute("success", "Học sinh đã được thêm thành công!");
        return "redirect:/DanhSachHocSinh";
    }


    @PostMapping("/ThemNhanVien")
    public String ThemNhanVien(@RequestParam String EmployeeID,
                               @RequestParam String FirstName,
                               @RequestParam String LastName,
                               @RequestParam String Email,
                               @RequestParam String PhoneNumber,
                               @RequestParam String Password,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {

        Persons existingEmployee = entityManager.find(Persons.class, EmployeeID);
        if (existingEmployee != null) {
            redirectAttributes.addFlashAttribute("error", "Mã nhân viên đã tồn tại!");
            return "redirect:/ThemNhanVien";
        }

        // Kiểm tra trùng Email
        boolean emailExists = !entityManager.createQuery("SELECT e FROM Persons e WHERE e.email = :email", Persons.class)
                .setParameter("email", Email)
                .getResultList().isEmpty();

        if (emailExists) {
            redirectAttributes.addFlashAttribute("error", "Email này đã tồn tại!");
            return "redirect:/ThemNhanVien";
        }

        // Kiểm tra trùng PhoneNumber
        boolean phoneExists = !entityManager.createQuery("SELECT e FROM Persons e WHERE e.phoneNumber = :phone", Persons.class)
                .setParameter("phone", PhoneNumber)
                .getResultList().isEmpty();

        if (phoneExists) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại này đã tồn tại!");
            return "redirect:/ThemNhanVien";
        }

        // Tạo nhân viên mới
        Staffs employees = new Staffs();
        employees.setId(EmployeeID);
        employees.setFirstName(FirstName);
        employees.setLastName(LastName);
        employees.setEmail(Email);
        employees.setPhoneNumber(PhoneNumber);
        employees.setPassword(Password);

        entityManager.persist(employees);

        redirectAttributes.addFlashAttribute("success", "Thêm nhân viên thành công!");
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
                        "SELECT s FROM Persons s WHERE s.email = :email AND s.id <> :id", Persons.class)
                .setParameter("email", email)
                .setParameter("id", id)
                .getResultList().isEmpty();

        if (emailExists) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng!");
            return "redirect:/SuaHocSinh/" + id;
        }

        // Kiểm tra số điện thoại trùng lặp với học sinh khác
        boolean phoneExists = !entityManager.createQuery(
                        "SELECT s FROM Persons s WHERE s.phoneNumber = :phoneNumber AND s.id <> :id", Persons.class)
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
            Staffs employee = entityManager.find(Staffs.class, employeeID);
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
        TypedQuery<Persons> emailQuery = entityManager.createQuery(
                "SELECT t FROM Persons t WHERE t.email = :email AND t.id <> :id", Persons.class);
        emailQuery.setParameter("email", email);
        emailQuery.setParameter("id", id);
        if (!emailQuery.getResultList().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại!");
            return "redirect:/SuaGiaoVien/" + id;
        }

        // Kiểm tra trùng số điện thoại (trừ giáo viên hiện tại)
        TypedQuery<Persons> phoneQuery = entityManager.createQuery(
                "SELECT t FROM Persons t WHERE t.phoneNumber = :phoneNumber AND t.id <> :id", Persons.class);
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
            Staffs employee = entityManager.find(Staffs.class, employeeID);
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
                                  @ModelAttribute Staffs updatedEmployee,
                                  RedirectAttributes redirectAttributes) {

        Staffs existingEmployee = entityManager.find(Staffs.class, id);
        if (existingEmployee == null) {
            redirectAttributes.addFlashAttribute("error", "Nhân viên không tồn tại!");
            return "redirect:/DanhSachNhanVien";
        }

        // Kiểm tra trùng Email
        TypedQuery<Persons> emailQuery = entityManager.createQuery(
                "SELECT e FROM Persons e WHERE e.email = :email AND e.id <> :id", Persons.class);
        emailQuery.setParameter("email", updatedEmployee.getEmail());
        emailQuery.setParameter("id", id);
        List<Persons> emailList = emailQuery.getResultList();
        if (!emailList.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại!");
            return "redirect:/SuaNhanVien/" + id;
        }

        // Kiểm tra trùng số điện thoại
        TypedQuery<Persons> phoneQuery = entityManager.createQuery(
                "SELECT e FROM Persons e WHERE e.phoneNumber = :phone AND e.id <> :id", Persons.class);
        phoneQuery.setParameter("phone", updatedEmployee.getPhoneNumber());
        phoneQuery.setParameter("id", id);
        List<Persons> phoneList = phoneQuery.getResultList();
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
            , ModelMap ModelMap, Model model) {
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
            model.addAttribute("keyword", keyword);
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
        model.addAttribute("keyword", keyword);
        model.addAttribute("teachers", searchResults);
        return "DanhSachTimKiemGiaoVien";
    }

    @PostMapping("/TimKiemNhanVien")
    public String TimKiemNhanVien(@RequestParam("searchType") String searchType,
                                  @RequestParam("keyword") String keyword,
                                  ModelMap model) {
        List<Staffs> searchResults;

        if (searchType.equalsIgnoreCase("name")) {
            searchResults = entityManager.createQuery(
                            "SELECT e FROM Staffs e " +
                                    "WHERE LOWER(e.firstName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(e.lastName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(:keyword)", Staffs.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
        } else if (searchType.equalsIgnoreCase("id")) {
            Staffs employee = entityManager.find(Staffs.class, keyword);
            searchResults = (employee != null) ? List.of(employee) : List.of();
        } else {
            searchResults = List.of();
        }
        model.addAttribute("keyword", keyword);

        model.addAttribute("employees", searchResults);
        return "DanhSachTimKiemNhanVien";
    }

    @PostMapping("/XoaTatCaHocSinh")
    public String xoaTatCaHocSinh() {
        entityManager.createQuery("DELETE FROM Students").executeUpdate();
        return "redirect:/DanhSachHocSinh";
    }

    @PostMapping("/XoaTatCaNhanVienWithAttributes")
    public String xoaTatCaNhanVien(@RequestParam("keyword") String keyword, RedirectAttributes redirectAttributes) {
        int deletedCount = entityManager.createQuery(
                        "DELETE FROM Staffs e WHERE LOWER(e.firstName) LIKE LOWER(:keyword) " +
                                "OR LOWER(e.lastName) LIKE LOWER(:keyword) " +
                                "OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(:keyword)")
                .setParameter("keyword", "%" + keyword + "%")
                .executeUpdate();

        redirectAttributes.addFlashAttribute("message", "Đã xóa " + deletedCount + " nhân viên.");
        return "redirect:/DanhSachNhanVien";
    }

    @PostMapping("/XoaTatCaHocSinhWithAttributes")
    public String xoaTatCaHocSinh(@RequestParam("keyword") String keyword, RedirectAttributes redirectAttributes) {
        int deletedCount = entityManager.createQuery(
                        "DELETE FROM Students s WHERE LOWER(s.firstName) LIKE LOWER(:keyword) " +
                                "OR LOWER(s.lastName) LIKE LOWER(:keyword) " +
                                "OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(:keyword)")
                .setParameter("keyword", "%" + keyword + "%")
                .executeUpdate();

        redirectAttributes.addFlashAttribute("message", "Đã xóa " + deletedCount + " học sinh.");
        return "redirect:/DanhSachHocSinh";
    }

    @PostMapping("/XoaTatCaGiaoVienWithAttributes")
    public String xoaTatCaGiaoVien(@RequestParam("keyword") String keyword, RedirectAttributes redirectAttributes) {
        int deletedCount = entityManager.createQuery(
                        "DELETE FROM Teachers t WHERE LOWER(t.firstName) LIKE LOWER(:keyword) " +
                                "OR LOWER(t.lastName) LIKE LOWER(:keyword) " +
                                "OR LOWER(CONCAT(t.firstName, ' ', t.lastName)) LIKE LOWER(:keyword)")
                .setParameter("keyword", "%" + keyword + "%")
                .executeUpdate();

        redirectAttributes.addFlashAttribute("message", "Đã xóa " + deletedCount + " giáo viên.");
        return "redirect:/DanhSachGiaoVien";
    }


}