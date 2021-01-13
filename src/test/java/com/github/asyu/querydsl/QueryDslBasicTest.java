package com.github.asyu.querydsl;

import static com.github.asyu.querydsl.entity.QMember.member;
import static com.github.asyu.querydsl.entity.QTeam.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.github.asyu.querydsl.entity.Member;
import com.github.asyu.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;


@SpringBootTest
@Transactional
public class QueryDslBasicTest {

	@Autowired
	EntityManager em;
	
	JPAQueryFactory queryFactory;
	
	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
	}
	
	@Test
	public void startJPQL() {
		// member1
		String qlString = "select m from Member m where m.username = :username";
		Member findMember = em.createQuery(qlString, Member.class)
									.setParameter("username", "member1")
									.getSingleResult();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void startQuerydsl() {
		Member findMember = queryFactory
								.select(member)
								.from(member)
								.where(member.username.eq("member1")) // 파라미터 바인딩 처리
								.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void search() {
		Member findMember = queryFactory
								.selectFrom(member)
								.where(member.username.eq("member1")
													  .and(member.age.between(10, 30)))
								.fetchOne()
								;
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
								.selectFrom(member)
								.where(member.username.eq("member1"), member.age.eq(10))
								.fetchOne()
								;
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void resultFetch() {
//		List<Member> fetch = queryFactory
//									.selectFrom(member)
//									.fetch()
//									;
//		
//		Member fetchOne = queryFactory
//									.selectFrom(member)
//									.fetchOne()
//									;
//		
//		Member fetchFirst = queryFactory
//				.selectFrom(member)
//				.fetchFirst()
//				;
		
//		QueryResults<Member> results = queryFactory
//											.selectFrom(member)
//											.fetchResults()
//											;
//		
//		results.getTotal();
//		List<Member> content = results.getResults();
		
		long total = queryFactory
							.selectFrom(member)
							.fetchCount();
	}
	
	/**
	 * 회원 정렬 순서
	 * 1. 회원 나이 내림차순 (desc)
	 * 2. 회원 이름 올림차순 (asc)
	 * 단 2에서 회원 이름이 없다면 마지막에 출력(nulls last) 
	 */
	@Test
	public void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));
		
		List<Member> result = queryFactory
				.selectFrom(member)
				.where(member.age.eq(100))
				.orderBy(member.age.desc(), member.username.asc().nullsLast())
				.fetch();
		
		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);
		
		Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
		Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
		Assertions.assertThat(memberNull.getUsername()).isNull();
	}
	
	@Test
	public void paging() {
		List<Member> result = queryFactory
									.selectFrom(member)
									.orderBy(member.username.desc())
									.offset(1)
									.limit(2)
									.fetch();
		
		Assertions.assertThat(result.size()).isEqualTo(2);
	}

	@Test
	public void paging2() {
		QueryResults<Member> result = queryFactory
									.selectFrom(member)
									.orderBy(member.username.desc())
									.offset(1)
									.limit(2)
									.fetchResults();
		
		Assertions.assertThat(result.getTotal()).isEqualTo(4);
		Assertions.assertThat(result.getLimit()).isEqualTo(2);
		Assertions.assertThat(result.getOffset()).isEqualTo(1);
		Assertions.assertThat(result.getResults().size()).isEqualTo(2);
	}
	
	@Test
	public void aggregation() {
		List<Tuple> result = queryFactory.select(member.count(), 
												member.age.sum(), 
												member.age.avg(),
												member.age.max(), 
												member.age.min())
										.from(member)
										.fetch()
										;
		
		Tuple tuple = result.get(0);
		Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
		Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
		Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}
	
	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라.
	 */
	@Test
	public void group() {
		List<Tuple> result = queryFactory.select(team.name, member.age.avg())
										.from(member)
										.join(member.team, team)
										.groupBy(team.name)
										.fetch();
		
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		
		Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
		Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2
		
		Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
		Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
	}
	
	@Test
	public void join() {
		List<Member> result = queryFactory.selectFrom(member)
											.join(member.team, team)
											.where(team.name.eq(("teamA")))
											.fetch();
							
		Assertions.assertThat(result).extracting("username").containsExactly("member1", "member2");
	}
}