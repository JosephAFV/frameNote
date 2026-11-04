package com.framenote.infrastructure;
import com.framenote.domain.Account;
import com.framenote.application.port.AccountStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<Account,String>, AccountStore { java.util.Optional<Account> findByEmail(String email); }

