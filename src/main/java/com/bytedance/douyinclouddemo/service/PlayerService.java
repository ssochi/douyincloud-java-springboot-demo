package com.bytedance.douyinclouddemo.service;

import com.bytedance.douyinclouddemo.entity.Player;
import com.bytedance.douyinclouddemo.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Transactional
    public Player createPlayer(Player player) {
        return playerRepository.save(player);
    }
    
    public Player findByUserId(String userId) {
        return playerRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Player not found"));
    }

}