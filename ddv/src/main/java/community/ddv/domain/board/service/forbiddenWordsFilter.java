package community.ddv.domain.board.service;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class forbiddenWordsFilter {

  private final RedisTemplate<String, String> redisTemplate;

  public forbiddenWordsFilter(@Qualifier("redisForbiddenWordsTemplate") RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public Set<String> getForbiddenWords() {
    return redisTemplate.opsForSet().members("forbidden-words");
  }

  public String filterForbiddenWords(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    Set<String> forbiddenWords = getForbiddenWords();
    for (String word : forbiddenWords) {
      if (input.contains(word)) {
        String masked = "*".repeat(word.length());
        input = input.replace(word, masked);
      }
    }
    return input;
  }
}

