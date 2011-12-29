package jsonapi

import grails.plugins.springsecurity.Secured
import grails.converters.JSON
import com.intelligrape.example.json.Book

@Secured(["ROLE_USER"])
class JsonController {

    def getBooks = {
        JSON.use("custom"){
            render Book.list() as JSON
        }
    }

    def getMap = {
        Map map = [a:13, b: Book.Genre.FICTION, c: 'ABC', d:['hey', 'hello']]
        render map as JSON
    }
    
    def getBooksSummary = {
        JSON.use("summary"){
            render Book.list() as JSON
        }
    }
}
