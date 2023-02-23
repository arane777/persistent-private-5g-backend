package com.hackathon.demo.controller;

import com.hackathon.demo.service.LinuxCommandExecutor;
import com.hackathon.demo.vo.RequestVO;
import com.hackathon.demo.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HackathonDemoController {
    @Autowired
    private LinuxCommandExecutor executor;
    
    @CrossOrigin
    @PostMapping("/executecommand")
    public ResponseVO executeLinuxComman(@RequestBody RequestVO requestVO){
        boolean result = false;
        ResponseVO response = new ResponseVO();
        
        if (requestVO != null && requestVO.getCommand()!= null) {
        //    result = executor.executeCommand(requestVO.getCommand());
            result = executor.runCommandOnRemoteServer(requestVO.getCommand());
            if (result) {
                response.setStatusCode("200");
                response.setStatusMessage("Command executed successfully");
            } else {
                response.setStatusCode("500");
                response.setStatusMessage("Exception occurred while executing command");
            }
            
        } else {
            response.setStatusCode("500");
            response.setStatusMessage("Invalid request body!!");
        }
        
        return response;
    }
}
