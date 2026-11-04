package com.framenote.infrastructure;
import com.framenote.domain.Workspace;
import com.framenote.application.port.WorkspaceStore;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface WorkspaceRepository extends JpaRepository<Workspace,String>, WorkspaceStore {
 java.util.List<Workspace> findByOwnerId(String ownerId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Workspace w where w.id=:id") java.util.Optional<Workspace> lockById(@Param("id") String id);
}

