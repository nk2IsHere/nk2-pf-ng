package eu.nk2.portfolio.impl.data

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.util.control.*
import eu.nk2.portfolio.util.misc.ALPHABET_AND_NUMBERS
import eu.nk2.portfolio.util.misc.randomOf
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

@Document(collection = "token")
data class Token(
    @Id val value: String,
    @CreatedDate val createdAt: Date = Date.from(Instant.now())
)

enum class TokenValidationResult {
    TOKEN_VALID,
    TOKEN_INVALID,
    TOKEN_EXPIRED
}

interface TokenServiceDependencies: DataDependencies

context(PortfolioConfigurationProperties, TokenServiceDependencies)
suspend fun tokenServiceGenerateToken(providedPassword: String): Option<Token> {
    if(providedPassword != tokenPassword)
        return nothing()

    return monoOption {
        mongoTemplate
            .save(Token(
                value = ALPHABET_AND_NUMBERS.randomOf(tokenLength)
            ))
    }
}

context(PortfolioConfigurationProperties, TokenServiceDependencies)
suspend fun tokenServiceValidateToken(tokenValue: String): Try<TokenValidationResult> {
    val tokenOption = monoTryOption {
        mongoTemplate
            .findOne<Token>(query(
                Criteria
                    .where("value")
                    .isEqualTo(tokenValue)
            ))
    }

    return tokenOption
        .fold(
            { it.rewrap() },
            { TokenValidationResult.TOKEN_INVALID.wrap },
            {
                if(it.createdAt.toInstant().plus(tokenExpiryMilliseconds, ChronoUnit.MILLIS) <= Instant.now()) TokenValidationResult.TOKEN_EXPIRED.wrap
                else TokenValidationResult.TOKEN_VALID.wrap
            }
        )
}
