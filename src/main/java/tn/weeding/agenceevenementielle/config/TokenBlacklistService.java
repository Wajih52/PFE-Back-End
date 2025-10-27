package tn.weeding.agenceevenementielle.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

 @Component
 @Slf4j
public class TokenBlacklistService {
     private static final String BLACKLIST_PREFIX = "blacklist:token:";

     @Autowired(required = false) // Ne pas échouer si Redis n'est pas disponible
     private RedisTemplate<String, String> redisTemplate;

     @Value("${jwt.expiration}")
     private long jwtExpiration;


     // Fallback en mémoire
     private final Set<String> inMemoryBlacklist = new HashSet<>();

     private boolean useRedis = false;


     @PostConstruct
     public void init() {
         // Détecter si Redis est disponible
         if (redisTemplate != null) {
             try {
                 redisTemplate.getConnectionFactory().getConnection().ping();
                 useRedis = true;
                 log.info("✅ Redis détecté et connecté - Mode REDIS activé");
             } catch (Exception e) {
                 log.warn("⚠️ Redis non disponible - Mode MÉMOIRE activé (fallback)");
                 useRedis = false;
             }
         } else {
             log.warn("⚠️ RedisTemplate non configuré - Mode MÉMOIRE activé");
             useRedis = false;
         }
     }

     /**
      * Ajoute un token à la blacklist
      */
     public void addToBlacklist(String token) {
         if (useRedis) {
             addToRedisBlacklist(token);
         } else {
             addToMemoryBlacklist(token);
         }
     }

     /**
      * Vérifie si un token est blacklisté
      */
     public boolean isBlacklisted(String token) {
         if (useRedis) {
             return isBlacklistedInRedis(token);
         } else {
             return isBlacklistedInMemory(token);
         }
     }

     // ========== REDIS METHODS ==========

     private void addToRedisBlacklist(String token) {
         String key = BLACKLIST_PREFIX + token;
         redisTemplate.opsForValue().set(
                 key,
                 "blacklisted",
                 jwtExpiration,
                 TimeUnit.MILLISECONDS
         );
         log.info("🔴 Token ajouté à la blacklist Redis avec TTL de {} ms", jwtExpiration);
     }

     public boolean isBlacklistedInRedis(String token) {
         String key = BLACKLIST_PREFIX + token;
         Boolean exists = redisTemplate.hasKey(key);
         return exists != null && exists;
     }

     // ========== IN-MEMORY METHODS ==========

     private void addToMemoryBlacklist(String token) {
         inMemoryBlacklist.add(token);
         log.info("🟡 Token ajouté à la blacklist en mémoire (mode fallback)");
     }

     public boolean isBlacklistedInMemory(String token) {
         return inMemoryBlacklist.contains(token);
     }

     // ========== UTILITY METHODS ==========

     public boolean isUsingRedis() {
         return useRedis;
     }

     public long countBlacklistedTokens() {
         if (useRedis) {
             return redisTemplate.keys(BLACKLIST_PREFIX + "*").size();
         } else {
             return inMemoryBlacklist.size();
         }
     }

     public void clearAllBlacklistedTokens() {
         if (useRedis) {
             redisTemplate.keys(BLACKLIST_PREFIX + "*")
                     .forEach(key -> redisTemplate.delete(key));
             log.info("🗑️ Blacklist Redis vidée");
         } else {
             inMemoryBlacklist.clear();
             log.info("🗑️ Blacklist mémoire vidée");
         }
     }
}
