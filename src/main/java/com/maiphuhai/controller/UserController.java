package com.maiphuhai.controller;

import com.maiphuhai.model.User;
import com.maiphuhai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /* ───────────────────── LIST ───────────────────── */
    @GetMapping
    public String listAll(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list";
    }

    /* ───────────────────── ADD ────────────────────── */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("mode", "ADD");
        return "users/user-form";
    }

    @PostMapping("/add")
    public String processAdd(@ModelAttribute("user") @Valid User user,
                             BindingResult binding,
                             RedirectAttributes ra) {

        if (binding.hasErrors()) {
            return "users/user-form";
        }
        userService.save(user);
        ra.addFlashAttribute("success", "Thêm user thành công!");
        return "redirect:/users";
    }

    /* ───────────────────── EDIT ───────────────────── */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model) {
        User user = userService.findById(id);
        if (user == null) return "redirect:/users";   // hoặc 404 page
        model.addAttribute("user", user);
        model.addAttribute("mode", "EDIT");
        return "users/user-form";
    }

    @PostMapping("/edit/{id}")
    public String processEdit(@PathVariable int id,
                              @ModelAttribute("user") @Valid User user,
                              BindingResult binding,
                              RedirectAttributes ra) {

        if (binding.hasErrors()) {
            return "users/user-form";
        }
        user.setUserId(id);
        userService.save(user);
        ra.addFlashAttribute("success", "Cập nhật user thành công!");
        return "redirect:/users";
    }

    /* ───────────────────── DELETE ─────────────────── */
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable int id,
                             RedirectAttributes ra) {
        userService.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa user!");
        return "redirect:/users";
    }
}



















