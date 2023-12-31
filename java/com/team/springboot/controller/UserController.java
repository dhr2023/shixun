package com.team.springboot.controller;


import com.mysql.cj.util.StringUtils;
import com.team.springboot.mapper.UserAddressMapper;
import com.team.springboot.pojo.*;

import com.team.springboot.service.AddressService;
import com.team.springboot.service.UserAddressService;
import com.team.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


@Controller
@RequestMapping("/admin")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    AddressService addressService;
    //后台初始化
    @RequestMapping("/userInfo")
    public String showUserInfo(Model m, HttpSession session) {
        String account = (String) session.getAttribute("u_Account");
        User user = userService.selectUserById(account);
        Address addr= userService.selectAddressAll(account);
        m.addAttribute("user", user);

        if(addr == null) {
            addressService.insertAddressOne(account,"无","无","无","无");
            addr = userService.selectAddressAll(account);
        }
        m.addAttribute("addressList",addr);
        return "admin/userInfo";
    }

    // 更新用户信息
    @RequestMapping(value = "/userUpdate", method = {RequestMethod.POST})
    @ResponseBody
    public BaseResponse updateUserInfo(@RequestBody User u) {
        BaseResponse<Integer> baseResponse = new BaseResponse<Integer>();

        if(u.getU_Phone().length() < 11 || u.getU_Name() == "" || u.getU_Email().indexOf("@") == -1){
            baseResponse.setCode(500);  // 前端所传内容不符合要求
            baseResponse.setMsg("保存失败");
        }else{
            baseResponse.setCode(200);
            baseResponse.setMsg("保存成功！");
            userService.updateUser(u);
        }

        return baseResponse;
    }

    // 更改密码
    @RequestMapping("/passwordUpdate")
    @ResponseBody
    public BaseResponse updatePassword(@RequestBody Password pojo){
        BaseResponse<Integer> baseResponse = new BaseResponse<Integer>();
        pojo.setU_Account("1001");
        String u_Account = pojo.getU_Account();
        String rightPassword = userService.selectPasswordById(u_Account); //通过账号查询正确的密码

        //修改失败！与原密码一致
        if(pojo.getNewPassword().equals(pojo.getNewPasswordAgain()) && pojo.getNewPassword().equals(pojo.getU_Password()) && pojo.getU_Password().equals(rightPassword)){
            baseResponse.setCode(500);
            baseResponse.setMsg("修改失败！与原密码一致");
            return baseResponse;
        }
        //修改失败！密码为空
        if(pojo.getU_Password().equals("") || pojo.getNewPassword().equals("") || pojo.getNewPasswordAgain().equals("")){
            baseResponse.setCode(500);
            baseResponse.setMsg("密码为空，请重新填写");
            return baseResponse;
        }
        // 修改失败！原密码不正确
        if(!pojo.getU_Password().equals(rightPassword)){
            baseResponse.setCode(500);
            baseResponse.setMsg("修改失败！原密码不正确");
            return baseResponse;
        }
        // 修改失败！两遍新密码不一致
        if(!pojo.getNewPassword().equals(pojo.getNewPasswordAgain())){
            baseResponse.setCode(500);
            baseResponse.setMsg("修改失败！两遍新密码不一致");
            return baseResponse;
        }
        //修改成功
        userService.updatePassword(pojo);
        baseResponse.setCode(200);
        baseResponse.setMsg("修改成功！");
        return baseResponse;
    }

    //寻找用户管理网页
    @RequestMapping("/userinit")
    public String userinit(){
        return "admin/user";
    }

    //生成用户表
    @RequestMapping("/userdata")
    @ResponseBody
    public BaseResponse userData (@RequestParam String page,
                                  @RequestParam String limit,
                                  HttpSession session){
        List<User> users;
        User u;
        BaseResponse<List<User>> baseResponse = new BaseResponse<>();
        if(session.getAttribute("u_Account").equals("admin")) {
            users = userService.selectUserAll(StringUtils.isNullOrEmpty(page) ? 1 : Integer.valueOf(page),
                    StringUtils.isNullOrEmpty(limit) ? 10 : Integer.valueOf(limit));
            baseResponse.setCount(userService.selectCount());
        }
        else{
            users=userService.selectUserByIdrtlist((String) session.getAttribute("u_Account"));
            baseResponse.setCount(1);
        }
        if(users!=null){
            baseResponse.setCode(200);
            baseResponse.setMsg("请求成功");
            baseResponse.setData(users);
        }
        else{
            baseResponse.setCode(500);
            baseResponse.setMsg("请求失败");
        }
        return baseResponse;
    }

    @RequestMapping("/userhead")
    public String userhead() {
        return "admin/uploadheadp";
    }
    //上传用户自定义头像
    @RequestMapping(value="/uploadSource" , method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse uploadSource(@RequestParam("file") MultipartFile file , HttpServletRequest request) throws IOException {
        BaseResponse baseResponse=new BaseResponse();
        System.out.println(file);
        String pathString = null;
        if(file!=null) {
            String path=request.getRealPath("/images/user/");
            String name= new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "_" +file.getOriginalFilename();
            pathString = path + name;
            File files=new File(pathString);
            //把内存图片写入磁盘中
            file.transferTo(files);
            String realPath = "/images/user/"+name;
            baseResponse.setCode(200);
            baseResponse.setMsg(pathString);
            User user=userService.selectUserById((String) request.getSession().getAttribute("u_Account"));

            //删除服务器里的文件
            File filesdelete =new File(request.getRealPath("/")+user.getU_Url());
            if(filesdelete.exists()&&!user.getU_Url().contains("default")) {
                filesdelete.delete();
                System.out.println("文件已经删除");
            }

            //更新数据库
            userService.updateHeadp(realPath, (String) request.getSession().getAttribute("u_Account"));
        }
        else{
            baseResponse.setCode(500);
            baseResponse.setMsg("出现错误");
        }
        return baseResponse;
    }
    //上传默认头像
    @RequestMapping(value="/uploaddefault")
    @ResponseBody
    public BaseResponse uploaddefault(@RequestBody User user , HttpServletRequest request) throws IOException {
        BaseResponse baseResponse=new BaseResponse();
        baseResponse.setCode(200);
        User u1=userService.selectUserById((String) request.getSession().getAttribute("u_Account"));
        //删除服务器里的文件
        File filesdelete =new File(request.getRealPath("/")+u1.getU_Url());
        if(filesdelete.exists()&& !u1.getU_Url().contains("default")) {
            filesdelete.delete();
            System.out.println("文件已经删除");
        }
        //更新数据库
        userService.updateHeadp(user.getU_Url(), (String) request.getSession().getAttribute("u_Account"));
        return baseResponse;
    }


    // 退出登录
    @RequestMapping("/quit")
    public String quit(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }

}
