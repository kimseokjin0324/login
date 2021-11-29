package hello.login.domain.member;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;

//-Meber객체를 저장하는 리포지토리가 필요함
/**
 * 동시성 문제가 고려되어 있지 않음, 실무에서는 ConcurrentHashMap, AtomicLong 사용 고려
 */
@Slf4j
@Repository
public class MemberRepository {

    private static Map<Long, Member> store=new HashMap<>();//static사용
    private static long sequence=0L;//static 사용

    public Member save(Member member){
        member.setId(++sequence);
        log.info("save:member={}",member);
        store.put(member.getId(),member);
        return member;
    }
    public Member findById(Long id){
        return store.get(id);

    }

//    public Member findByLoginId(String loginId){
//        List<Member> all = findAll();
//        for(Member m:all){
//            if(m.getLoginId().equals(loginId)) {
//                return m;
//            }
//
//        }
//       return null;
//    }
    //-위 코드와 동일한 코드
        public Optional<Member> findByLoginId(String loginId){
//        List<Member> all = findAll();
//        for(Member m:all){
//            if(m.getLoginId().equals(loginId)) {
//                return Optional.of(m);
//            }
//        }
//            return Optional.empty();
            //-위 코드와 동일
            //- 리스트를 stream으로 바꿈 -> filter안에 조건에 만족하는 것만 다음단계로 넘겨짐->findfirst()먼저 나온애들을 반환함함
             return findAll().stream()
                .filter(m->m.getLoginId().equals(loginId))
                .findFirst();
    }

    public List<Member> findAll(){
       return new ArrayList<>(store.values());
    }

    public void clearStore(){
        store.clear();
    }
}
