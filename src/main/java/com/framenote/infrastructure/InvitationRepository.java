package com.framenote.infrastructure;
import com.framenote.domain.Invitation;
import com.framenote.application.port.InvitationStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InvitationRepository extends JpaRepository<Invitation,String>, InvitationStore { java.util.Optional<Invitation> findByTokenHash(String tokenHash); }

