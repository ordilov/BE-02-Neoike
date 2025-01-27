package prgrms.neoike.service.converter;

import prgrms.neoike.domain.member.Email;
import prgrms.neoike.domain.member.Member;
import prgrms.neoike.domain.member.Password;
import prgrms.neoike.domain.member.PhoneNumber;
import prgrms.neoike.service.dto.memberdto.MemberDto;
import prgrms.neoike.service.dto.memberdto.MemberResponse;

public class MemberConverter {

    public static Member toMember(MemberDto memberDto) {
        return Member.builder()
                .name(memberDto.name())
                .password(new Password(memberDto.password()))
                .phoneNumber(new PhoneNumber(memberDto.countryCode(), memberDto.phoneNumber()))
                .birthDay(memberDto.birthday())
                .email(new Email(memberDto.email()))
                .address(memberDto.address())
                .gender(memberDto.gender())
                .build();
    }

    public static MemberResponse toMemberResponse(Long memberId, String email) {
        return new MemberResponse(memberId, email);
    }
}
