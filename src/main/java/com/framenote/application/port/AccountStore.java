package com.framenote.application.port;

import com.framenote.domain.Account;

public interface AccountStore {
    java.util.Optional<Account> findById(String id);
    <S extends Account> S save(S entity);
    java.util.Optional<Account> findByEmail(String email);
}
