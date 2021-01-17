package com.github.asyu.querydsl.repository;

import com.github.asyu.querydsl.dto.MemberSearchCondition;
import com.github.asyu.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);
}
