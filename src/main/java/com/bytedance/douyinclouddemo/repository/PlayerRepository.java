package com.bytedance.douyinclouddemo.repository;

import com.bytedance.douyinclouddemo.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    
    Optional<Player> findByUserId(String userId);
    
    Optional<Player> findByUserName(String userName);

    @Modifying
    @Transactional
    default int batchUpdatePlayers(List<Player> players) {
        List<Player> rst = saveAll(players);
        return rst.size();
    }
}
