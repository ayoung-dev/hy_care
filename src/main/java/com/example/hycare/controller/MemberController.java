package com.example.hycare.controller;

import com.example.hycare.Service.DiagnosisService;
import com.example.hycare.Service.MemberService;
import com.example.hycare.dto.DiagnosisDto;
import com.example.hycare.dto.MemberDto;
import com.example.hycare.entity.ApiResult;
import com.example.hycare.entity.ResultEntity;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final DiagnosisService diagnosisService;

    // 회원가입 및 로그인
    @PostMapping("/login")
    public ResultEntity memLogin(@RequestBody MemberDto memberDto) {
        ResultEntity result = new ResultEntity();
        try {
            if(memValid(memberDto.getEmail(), memberDto.getIsDoctor())) {
                // 회원이 존재하지 않으면 DB 저장
                memberService.saveMember(memberDto);

                // 회원가입 및 로그인 성공 반환
                result.setCode("0000");
                result.setMessage("Join and Login Success");
                log.info("Join and Login Success");
            } else {
                // 로그인 성공 반환
                result.setCode("0000");
                result.setMessage("Login Success");
                log.info("Login Success");
            }
            return result;
        } catch (Exception e) {
            log.error("Login or Join Fail");
            return new ResultEntity(e);
        }
    }

    // email로 회원 찾기
//    @GetMapping("/find-email/{email}")
//    public ResultEntity findMemByEmail(@PathVariable("email") String email) {
//        ResultEntity result = new ResultEntity(ApiResult.SUCCESSS);
//        try {
//            MemberDto member = memberService.findByEmail(email);
//            if(member.getEmail() == null) { // email에 해당하는 회원이 없는 경우
//               result.setCode(ApiResult.USER_NOT_FOUND.getCode());
//               result.setMessage(ApiResult.USER_NOT_FOUND.getMessage());
//               result.setData(null);
//                log.warn("Member not found");
//                return result;
//            }
//            result.setData(member);
//        } catch (Exception e) {
//            log.error("Find member fail");
//            result = new ResultEntity(e);
//        }
//        log.info("Find member success");
//        return result;
//    }

    // 진료 완료 -> member의 diagId 업데이트
    @PostMapping("update/{diagnosisId}")
    public ResultEntity updateDiagId(@RequestBody MemberDto member, @PathVariable("diagnosisId")String diagnosisId) {
        ResultEntity result = new ResultEntity(ApiResult.SUCCESSS);
        MemberDto memberDto = new MemberDto();
        List<String> diagIdList = new ArrayList<String>();
        try {
            memberDto = memberService.findByEmail(member.getEmail(), member.getIsDoctor());
            if(memberDto != null) {     // 회원 존재
                // 기존 diagId에 추가
                diagIdList.addAll(memberDto.getDiagId());
                diagIdList.add(diagnosisId);
                memberDto.setDiagId(diagIdList);
                memberService.addDiagId(memberDto);
                result.setData(memberDto);
            } else {    // 회원 존재X
                result.setCode(ApiResult.USER_NOT_FOUND.getCode());
                result.setMessage(ApiResult.USER_NOT_FOUND.getMessage());
                result.setData(null);
                log.warn("Member not found");
                return result;
            }
        } catch (Exception e) {
            log.error("Member diagId update fail");
            return new ResultEntity(e);
        }
        log.info("Member diagId update success");
        return result;
    }

    // 진료 기록 조회
    @GetMapping("/find-diag/{email}")
    public ResultEntity<List<DiagnosisDto>> findDiag(@PathVariable String email, @RequestParam String loginDiv) {
        log.info("Member diagnosis find");
        String isDoctor = loginDiv.equals("0") ? "D" : "P";
        MemberDto memberDto = memberService.findByEmail(email, isDoctor);
        List<DiagnosisDto> DiagnosisList = new ArrayList<>();
        if (memberDto.getDiagId() != null && !memberDto.getDiagId().isEmpty() && !"<null>".equals(memberDto.getDiagId().get(0))) {
            for(String diagId : memberDto.getDiagId()) {
                DiagnosisList.add(diagnosisService.findData(diagId));
            }
        }
        return new ResultEntity<>(DiagnosisList);
    }

    // 환자 이름으로 검색
    @GetMapping("/search-patient/{email}")
    public ResultEntity<List<DiagnosisDto>> searchByPatientName(@PathVariable String email, @RequestParam String patientName, String loginDiv) {
        log.info("Search by patient name");
        String isDoctor = loginDiv.equals("0") ? "D" : "P";
        MemberDto memberDto = memberService.findByEmail(email, isDoctor);
        List<DiagnosisDto> DiagnosisList = new ArrayList<>();
        if (memberDto.getDiagId() != null && !memberDto.getDiagId().isEmpty() && !"<null>".equals(memberDto.getDiagId().get(0))) {
            for (String diagId : memberDto.getDiagId()) {
                DiagnosisDto diagnosisDto = diagnosisService.findData(diagId);
                if(diagnosisDto.getPatientName() != null && diagnosisDto.getPatientName().contains(patientName)) {
                    DiagnosisList.add(diagnosisDto);
                }
            }
        }

        return new ResultEntity<>(DiagnosisList);
    }

    // 의사 이름으로 검색
    @GetMapping("/search-doctor/{email}")
    public ResultEntity<List<DiagnosisDto>> searchByDoctorName(@PathVariable String email, @RequestParam String doctorName, String loginDiv) {
        log.info("Search by doctor name");
        String isDoctor = loginDiv.equals("0") ? "D" : "P";
        MemberDto memberDto = memberService.findByEmail(email, isDoctor);
        List<DiagnosisDto> DiagnosisList = new ArrayList<>();
        if (memberDto.getDiagId() != null && !memberDto.getDiagId().isEmpty() && !"<null>".equals(memberDto.getDiagId().get(0))) {
            for (String diagId : memberDto.getDiagId()) {
                DiagnosisDto diagnosisDto = diagnosisService.findData(diagId);
                if(diagnosisDto.getDoctorName() != null && diagnosisDto.getDoctorName().contains(doctorName)) {
                    DiagnosisList.add(diagnosisDto);
                }
            }
        }

        return new ResultEntity<>(DiagnosisList);
    }

    // 기간으로 검색
    @GetMapping("/search-date/{email}")
    public ResultEntity<List<DiagnosisDto>> searchByDate(@PathVariable String email, @RequestParam String date1, String date2, String loginDiv) {
        log.info("Search by patient name");
        String isDoctor = loginDiv.equals("0") ? "D" : "P";
        MemberDto memberDto = memberService.findByEmail(email, isDoctor);
        List<DiagnosisDto> DiagnosisList = new ArrayList<>();
        if (memberDto.getDiagId() != null && !memberDto.getDiagId().isEmpty() && !"<null>".equals(memberDto.getDiagId().get(0))) {
            for (String diagId : memberDto.getDiagId()) {
                DiagnosisDto diagnosisDto = diagnosisService.findData(diagId);
                if(diagnosisDto.getDiagTime() != null) {
                    // SimpleDateFormat을 사용하여 timestamp를 문자열로 변환
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String formatDiagTime = dateFormat.format(diagnosisDto.getDiagTime());

                    // 날짜 비교
                    int resultDate1 = formatDiagTime.compareTo(date1);
                    int resultDate2 = formatDiagTime.compareTo(date2);
                    if(resultDate1 >= 0 && resultDate2 <= 0) {  // 검색하고자 하는 기간 안에 있으면 list에 추가
                        DiagnosisList.add(diagnosisDto);
                    }
                }
            }
        }
        return new ResultEntity<>(DiagnosisList);
    }

    // 회원이 존재하는지 validation
    public Boolean memValid(String email, String dtoIsDoctor) {
        log.info("Find Member");
        String isDoctor = dtoIsDoctor.equals("0") ? "D" : "P";
        MemberDto member = memberService.findByEmail(email, isDoctor);
        if(member.getEmail() == null || !member.getIsDoctor().equals(isDoctor)) {   // 회원 존재X
            return true;
        }
        return false;
    }
}
