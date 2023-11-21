package com.example.hycare.controller;

import com.example.hycare.Service.DiagnosisService;
import com.example.hycare.Service.MemberService;
import com.example.hycare.dto.DiagnosisDto;
import com.example.hycare.dto.MemberDto;
import com.example.hycare.entity.ApiResult;
import com.example.hycare.entity.ResultEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final DiagnosisService diagnosisService;

    @Value("${server.host.api}")
    private String baseUrl;

    // 회원가입 및 로그인
    @PostMapping("/login")
    public ResultEntity memLogin(@RequestBody MemberDto memberDto) {
        ResultEntity result = new ResultEntity();
        try {
            if(memValid(memberDto.getEmail())) {
                // 회원이 존재하지 않으면 DB 저장
                String[] email = memberDto.getEmail().split("@");
                memberDto.setMemName(email[0]);
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
    @GetMapping("/find-email/{email}")
    public ResultEntity findMemByEmail(@PathVariable("email") String email) {
        ResultEntity result = new ResultEntity(ApiResult.SUCCESSS);
        try {
            MemberDto member = memberService.findByEmail(email);
            if(member.getEmail() == null) { // email에 해당하는 회원이 없는 경우
               result.setCode(ApiResult.USER_NOT_FOUND.getCode());
               result.setMessage(ApiResult.USER_NOT_FOUND.getMessage());
               result.setData(null);
                log.warn("Member not found");
                return result;
            }
            result.setData(member);
        } catch (Exception e) {
            log.error("Find member fail");
            result = new ResultEntity(e);
        }
        log.info("Find member success");
        return result;
    }

    // 진료 완료 -> member의 diagId 업데이트
    @PostMapping("update/{id}")
    public ResultEntity updateDiagId(@PathVariable("id") int id, @RequestBody String diagnosisId) {
        ResultEntity result = new ResultEntity(ApiResult.SUCCESSS);
        MemberDto memberDto = new MemberDto();
        List<String> diagIdList = new ArrayList<String>();
        try {
            memberDto = memberService.findById(id);
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
    @GetMapping("/find-dig/{id}")
    public ResultEntity<List<DiagnosisDto>> findDiag(@PathVariable int id) {
        MemberDto memberDto = memberService.findById(id);
        List<DiagnosisDto> DiagnosisList = new ArrayList<>();
        for(String diagId : memberDto.getDiagId()) {
            DiagnosisList.add(diagnosisService.findData(diagId));
        }

        return new ResultEntity<>(DiagnosisList);
    }

    // 회원이 존재하는지 validation
    public Boolean memValid(String email) {
        log.info("Find Member");
        MemberDto member = memberService.findByEmail(email);
        if(member.getEmail() == null) {   // 회원 존재X
            return true;
        }
        return false;
    }

    @GetMapping("find-member")
    @Cacheable(value = "memberCache")
    public MemberDto findMember(@RequestBody String email) {
        MemberDto member = memberService.findByEmail(email);
        return member;
    }
}
