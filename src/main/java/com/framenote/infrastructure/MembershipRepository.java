package com.framenote.infrastructure;
import com.framenote.domain.Membership;
import com.framenote.application.port.MembershipStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MembershipRepository extends JpaRepository<Membership,String>, MembershipStore { java.util.List<Membership> findByWorkspaceId(String workspaceId); java.util.List<Membership> findByAccountId(String accountId); java.util.Optional<Membership> findByWorkspaceIdAndAccountId(String workspaceId,String accountId); }

