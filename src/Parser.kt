class Parser(private val lexer: Lexer) {
    private lateinit var currentToken: Token
    private lateinit var iterator: Iterator<Token>
    private lateinit var previousToken: Token
    private val globalVariables = mutableSetOf<String>()
    private val scopes = mutableListOf<MutableSet<String>>()

    fun parse(input: String) {
        val tokens = lexer.tokenize(input)
        iterator = tokens.iterator()

        if (iterator.hasNext()) {
            currentToken = iterator.next()
        } else {
            throw SyntaxErrorException("Erro: arquivo de entrada vazio.")
        }

        PROG()

        if (iterator.hasNext()) {
            throw SyntaxErrorException("Erro sintático: tokens não consumidos encontrados no fim do arquivo.")
        } else {
            println("Análise sintática concluída.")
        }
    }

    private fun consume(expectedType: TokenType, expectedLexeme: String? = null) {
        if (currentToken.type == TokenType.RESERVED_WORD && currentToken.lexeme == "program") {
            currentToken = iterator.next()
            if (currentToken.type == TokenType.IDENTIFIER) {
                currentToken = iterator.next()
                if (currentToken.lexeme == ";") {
                    currentToken = iterator.next()
                } else {
                    throw SyntaxErrorException("Erro sintático: ponto e vírgula esperado após o identificador do programa.")
                }
                return
            } else {
                throw SyntaxErrorException("Erro sintático: identificador esperado após 'program'.")
            }
        }

        println("Lendo: ${currentToken.type} '${currentToken.lexeme}' (esperado: $expectedType '${expectedLexeme ?: ""}')")
        if (currentToken.type != expectedType || (expectedLexeme != null && currentToken.lexeme != expectedLexeme)) {
            throw SyntaxErrorException("Erro sintático na linha ${currentToken.lineNumber}: esperado $expectedType${if (expectedLexeme != null) " '$expectedLexeme'" else ""}, encontrado ${currentToken.type} '${currentToken.lexeme}'.")
        }

        if (expectedType == TokenType.IDENTIFIER) {
            if (previousToken.type == TokenType.RESERVED_WORD && previousToken.lexeme in listOf("int", "float", "char", "string")) {
                declareVariable(currentToken.lexeme)
            } else if (previousToken.lexeme != "=" && !isVariableDeclared(currentToken.lexeme)) {
                throw SyntaxErrorException("Erro sintático na linha ${currentToken.lineNumber}: variável '${currentToken.lexeme}' não está declarada.")
            }
        }

        if (expectedType == TokenType.SPECIAL_SYMBOLS) {
            when (currentToken.lexeme) {
                "{" -> enterScope()
                "}" -> exitScope()
            }
        }

        previousToken = currentToken

        if (iterator.hasNext()) {
            currentToken = iterator.next()
        }
    }

    private fun enterScope() {
        scopes.add(mutableSetOf())
    }

    private fun exitScope() {
        if (scopes.isNotEmpty()) {
            scopes.removeAt(scopes.lastIndex)
        }
    }

    private fun declareVariable(variableName: String) {
        if (scopes.isNotEmpty()) {
            scopes.last().add(variableName)
        } else {
            globalVariables.add(variableName)
        }
    }

    private fun isVariableDeclared(variableName: String): Boolean {
        for (scope in scopes.reversed()) {
            if (scope.contains(variableName)) {
                return true
            }
        }
        return globalVariables.contains(variableName)
    }

    private fun checkVariableScope(variableName: String) {
        if (!isVariableDeclared(variableName)) {
            throw SyntaxErrorException("Erro sintático na linha ${currentToken.lineNumber}: variável '$variableName' não está declarada.")
        }
    }

    private fun WORD(): String? {
        return when (currentToken.type) {
            TokenType.IDENTIFIER -> {
                val word = currentToken.lexeme
                checkVariableScope(word)
                consume(TokenType.IDENTIFIER)
                word
            }

            TokenType.INT -> {
                consume(TokenType.INT)
                "int"
            }

            TokenType.FLOAT -> {
                consume(TokenType.FLOAT)
                "float"
            }

            TokenType.STRING -> {
                consume(TokenType.STRING)
                "string"
            }

            TokenType.CHARACTER -> {
                consume(TokenType.CHARACTER)
                "char"
            }

            else -> throw SyntaxErrorException("Erro sintático: esperado palavra, encontrado ${currentToken.lexeme}.")
        }
    }

    private fun TYPE() {
        consume(TokenType.RESERVED_WORD)
    }

    private fun ARGS() {
        TYPE()
        consume(TokenType.IDENTIFIER)
        if (currentToken.lexeme == ",") {
            consume(TokenType.SPECIAL_SYMBOLS) // ,
            ARGS()
        }
    }

    private fun DEC() {
        val variableType = currentToken.lexeme
        TYPE()
        val variableName = currentToken.lexeme
        consume(TokenType.IDENTIFIER)

        if (currentToken.lexeme == "(") {
            FUN()
        } else if (currentToken.lexeme == "=") {
            consume(TokenType.SPECIAL_SYMBOLS, "=")
            WORD()
            if (currentToken.lexeme == "(") {
                consume(TokenType.SPECIAL_SYMBOLS, "(")
                if (currentToken.lexeme != ")"){
                    WORD()
                    while (currentToken.lexeme == ",") {
                        consume(TokenType.SPECIAL_SYMBOLS, ",")
                        WORD()
                    }
                }
                consume(TokenType.SPECIAL_SYMBOLS, ")")
            }
            consume(TokenType.SPECIAL_SYMBOLS, ";")
        } else {
            consume(TokenType.SPECIAL_SYMBOLS, ";")
        }
    }

    private fun LID() {
        consume(TokenType.IDENTIFIER)
        if (currentToken.lexeme == ",") {
            consume(TokenType.SPECIAL_SYMBOLS)
            LID()
        }
    }

    private fun CALL() {
        consume(TokenType.IDENTIFIER)
        consume(TokenType.SPECIAL_SYMBOLS, "(")
        var balance = 1
        while (balance > 0 && iterator.hasNext()) {
            currentToken = iterator.next()
            if (currentToken.lexeme == "(") balance++
            else if (currentToken.lexeme == ")") balance--
            if (balance > 0) consume(currentToken.type)
        }
        consume(TokenType.SPECIAL_SYMBOLS, ")")
    }

    private fun CMD() {
        when (currentToken.type) {
            TokenType.RESERVED_WORD -> when (currentToken.lexeme) {
                "print" -> {
                    consume(TokenType.RESERVED_WORD, "print")
                    consume(TokenType.SPECIAL_SYMBOLS, "(")
                    WORD()
                    consume(TokenType.SPECIAL_SYMBOLS, ")")
                    consume(TokenType.SPECIAL_SYMBOLS, ";")
                }

                "int", "float", "char", "string" -> DEC()
                "if" -> IF()
                "while" -> LOOP()
                "return" -> RET()
                else -> throw SyntaxErrorException("Erro sintático: palavra reservada inesperada '${currentToken.lexeme}'.")
            }

            TokenType.IDENTIFIER -> {
                CALL()
                consume(TokenType.SPECIAL_SYMBOLS, ";")
            }

            TokenType.SPECIAL_SYMBOLS -> {
                if (currentToken.lexeme == ";") {
                    consume(TokenType.SPECIAL_SYMBOLS, ";")
                } else if (currentToken.lexeme == "{") {
                    consume(TokenType.SPECIAL_SYMBOLS, "{")
                    LISTC()
                    consume(TokenType.SPECIAL_SYMBOLS, "}")
                } else {
                    throw SyntaxErrorException("Erro sintático: símbolo especial inesperado '${currentToken.lexeme}'.")
                }
            }

            else -> throw SyntaxErrorException("Erro sintático: esperado comando, encontrado '${currentToken.lexeme}'.")
        }
    }

    private fun LISTC() {
        CMD()
        while (iterator.hasNext() && currentToken.lexeme != "}") {
            CMD()
        }
    }

    private fun FUN() {
        consume(TokenType.SPECIAL_SYMBOLS, "(")
        if (currentToken.lexeme != ")") {
            ARGS()
        }
        consume(TokenType.SPECIAL_SYMBOLS, ")")
        consume(TokenType.SPECIAL_SYMBOLS, "{")
        LISTC()
        consume(TokenType.SPECIAL_SYMBOLS, "}")
    }

    private fun OP() {
        consume(TokenType.RELATIONAL_SYMBOLS)
    }

    private fun REL() {
        WORD()
        if (currentToken.type == TokenType.RELATIONAL_SYMBOLS) {
            OP()
            REL()
        } else if (currentToken.lexeme == "(") {
            consume(TokenType.SPECIAL_SYMBOLS)
            REL()
            consume(TokenType.SPECIAL_SYMBOLS)
        }
    }

    private fun IF() {
        consume(TokenType.RESERVED_WORD, "if")
        consume(TokenType.SPECIAL_SYMBOLS, "(")
        REL()
        consume(TokenType.SPECIAL_SYMBOLS, ")")
        consume(TokenType.SPECIAL_SYMBOLS, "{")
        LISTC()
        consume(TokenType.SPECIAL_SYMBOLS, "}")
        if (currentToken.lexeme == "else") {
            consume(TokenType.RESERVED_WORD, "else")
            consume(TokenType.SPECIAL_SYMBOLS, "{")
            LISTC()
            consume(TokenType.SPECIAL_SYMBOLS, "}")
        }
    }

    private fun LOOP() {
        consume(TokenType.RESERVED_WORD, "while")
        consume(TokenType.SPECIAL_SYMBOLS, "(")
        REL()
        consume(TokenType.SPECIAL_SYMBOLS, ")")
        consume(TokenType.SPECIAL_SYMBOLS, "{")
        LISTC()
        consume(TokenType.SPECIAL_SYMBOLS, "}")
    }

    private fun RET() {
        consume(TokenType.RESERVED_WORD, "return")
        WORD()
        consume(TokenType.SPECIAL_SYMBOLS, ";")
    }

    fun PROG() {
        try {
            consume(TokenType.RESERVED_WORD, "program")

            while (iterator.hasNext()) {
                when (currentToken.type) {
                    TokenType.RESERVED_WORD -> when (currentToken.lexeme) {
                        "int", "float", "char", "string" -> DEC()
                        "void" -> FUN()
                        else -> break
                    }
                    TokenType.IDENTIFIER -> CMD()
                    TokenType.SPECIAL_SYMBOLS -> {
                        if (currentToken.lexeme == ";") {
                            consume(TokenType.SPECIAL_SYMBOLS, ";")
                        } else if (currentToken.lexeme == "}") {
                            break
                        } else {
                            throw SyntaxErrorException("Erro sintático: símbolo especial inesperado '${currentToken.lexeme}'.")
                        }
                    }
                    else -> throw SyntaxErrorException("Erro sintático: token inesperado '${currentToken.lexeme}'.")
                }
            }

            if (!iterator.hasNext()) {
                println("Análise sintática concluída: Programa válido.")
            } else {
                throw SyntaxErrorException("Erro durante a análise sintática: Fim de arquivo inesperado.")
            }
        } catch (e: SyntaxErrorException) {
            println("Erro durante a análise sintática: ${e.message}")
        }
    }

    class SyntaxErrorException(message: String) : Exception(message)
}
