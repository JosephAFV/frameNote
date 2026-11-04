package com.framenote.application.port;

import com.framenote.domain.Membership;

public interface MembershipStore {
    java.util.Optional<Membership> findById(String id);
    <S extends Membership> S save(S entity);
    java.util.List<Membership> findByWorkspaceId(String workspaceId);
    java.util.List<Membership> findByAccountId(String accountId);
    java.util.Optional<Membership> findByWorkspaceIdAndAccountId(String workspaceId,String accountId);
}
