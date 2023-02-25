package com.hackathon.demo.controller;

import com.hackathon.demo.service.LinuxCommandExecutor;
import com.hackathon.demo.vo.RequestVO;
import com.hackathon.demo.vo.ResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HackathonDemoController {
    Logger logger = LoggerFactory.getLogger(HackathonDemoController.class);
    @Autowired
    private LinuxCommandExecutor executor;

    @Value("${remote.server.metricsURL_1}")
    private String metricsURI_1;

    @Value("${remote.server.metricsURL_2}")
    private String metricsURI_2;

    @CrossOrigin
    @PostMapping("/executecommandsync")
    public ResponseVO executeLinuxCommandLocalSync(@RequestBody RequestVO requestVO) {
        logger.info("executeLinuxCommandLocalSync");
        boolean result = false;
        ResponseVO response = new ResponseVO();

        if (requestVO != null && requestVO.getCommand() != null) {
            result = executor.executeCommand(requestVO.getCommand());
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

    @CrossOrigin
    @PostMapping("/executecommand")
    public ResponseVO executeLinuxCommandLocalAsync(@RequestBody RequestVO requestVO) {
        boolean result = false;
        ResponseVO response = new ResponseVO();

        if (requestVO != null && requestVO.getCommand() != null) {

            logger.info("Controller: Execute Command " + requestVO.getCommand());
            executor.executeCommandAsync(requestVO.getCommand());
            response.setStatusCode("200");
            response.setStatusMessage("Command executed successfully");
            
        } else {
            response.setStatusCode("500");
            response.setStatusMessage("Invalid request body!!");
        }

        return response;
    }

    @CrossOrigin
    @GetMapping("/metrics/1")
    public ResponseVO getMetricsData() {
        logger.info("Controller: getMetricsData-1 " );
        boolean result = false;
        ResponseVO response = new ResponseVO();
        String responseData = executor.getMetricsData(metricsURI_1);
        if (responseData == null) {
            response.setStatusCode("500");
            response.setStatusMessage("Metrics API not reachable");
        } else {
            response.setStatusCode("200");
            response.setStatusMessage("Metrics Data fetched Successfully");
            response.setResponseData(mapResponseData(responseData));
        }

        return response;
    }

    @CrossOrigin
    @GetMapping("/metrics/2")
    public ResponseVO getMetricsData_2() {
        logger.info("Controller: getMetricsData-2 ");
        boolean result = false;
        ResponseVO response = new ResponseVO();
        String responseData = executor.getMetricsData(metricsURI_2);
        if (responseData == null) {
            response.setStatusCode("500");
            response.setStatusMessage("Metrics API not reachable");
        } else {
            response.setStatusCode("200");
            response.setStatusMessage("Metrics Data fetched Successfully");
            response.setResponseData(mapResponseData(responseData));
        }

        return response;
    }

    private Map<String, String> mapResponseData(String responseData) {
        logger.info("Metrics data:  "+ responseData);
        Map<String, String> resultMap = new HashMap<>();
        String[] lines2 = responseData.split("\n");
        Arrays.stream(lines2)
                .filter(line -> (line != null && !line.equals("")))
                .filter(line -> !line.contains("#"))
                .forEach(line ->
                        {
                            if (line != null || !line.equals("")) {
                                String[] resultArray = line.split(" ");
                                if (resultArray.length >= 2) {
                                    resultMap.put(resultArray[0], resultArray[1]);
                                }
                            }
                        }
                );
        logger.info("Mapping Metrics Data with counters  ");
        resultMap.entrySet().forEach((entry) ->  logger.info("" + entry.getKey() + " " + entry.getValue()));
        return resultMap;
    }


    @CrossOrigin
    @PostMapping("/executecommandremote")
    public ResponseVO executeLinuxCommand(@RequestBody RequestVO requestVO) {
        logger.info("Controller: executecommandremote ");
        boolean result = false;
        ResponseVO response = new ResponseVO();

        if (requestVO != null && requestVO.getCommand() != null) {
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



//    @CrossOrigin
//    @PostMapping("/executecommandnew")
//    public ResponseVO executeLinuxCommannew(@RequestBody RequestVO requestVO){
//        boolean result = false;
//        ResponseVO response = new ResponseVO();
//
//        if (requestVO != null && requestVO.getCommand()!= null) {
//            try {
//                String responseData = executor.runMutipleCommandsOnRemoteServer(requestVO.getCommand(), null);
//                if(responseData == null) {
//                    response.setStatusCode("500");
//                    response.setStatusMessage("Some exception occured while executing the command");
//                } else {
//                    System.out.println("ResponseData: "+responseData);
//                    response.setStatusCode("200");
//                    response.setStatusMessage("Command executed successfully");
//                }
//                
//            } catch (Exception e) {
//                response.setStatusCode("500");
//                response.setStatusMessage("Exception occurred while executing command");
//                System.out.println("Exception occured while executing command:"+e); ;
//            }
//
//        } else {
//            response.setStatusCode("500");
//            response.setStatusMessage("Invalid request body!!");
//        }
//
//        return response;
//    }


}
