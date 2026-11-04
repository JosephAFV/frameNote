package com.framenote.application.port;

import com.framenote.domain.Invitation;

public interface InvitationStore {
    java.util.Optional<Invitation> findById(String id);
    <S extends Invitation> S save(S entity);
    java.util.Optional<Invitation> findByTokenHash(String tokenHash);
}
