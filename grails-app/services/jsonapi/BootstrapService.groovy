package jsonapi

import com.intelligrape.example.json.User
import com.intelligrape.example.json.Role
import com.intelligrape.example.json.UserRole
import com.intelligrape.example.json.Book
import grails.converters.JSON
import com.intelligrape.example.json.CustomDomainClassJSONMarshaller
import com.intelligrape.example.json.JSONConstants

class BootstrapService {

    static transactional = true

    def grailsApplication

    def createRoles() {
        if (Role.count() == 0) {
            new Role(authority: 'ROLE_USER').save()
        }
    }

    public void createDefaultUser() {
        if (User.count() == 0) {
            createUserIfNotExists("vivek@example.com", "Vivek", "password")
        }
    }

    private void createUserIfNotExists(String email, String name, String password) {
        User user
        if (!User.findByUsername(email)) {
            user = new User(username: email, password: password,
                    accountLocked: false, enabled: true)
            String role = 'ROLE_USER'
            user.save(failOnError: true)
            UserRole.create user, Role.findByAuthority(role)
        }
    }

    public void createDefaultBooks(){
        new Book(name: "Tale of Two Cities", genre: Book.Genre.FICTION, isbn: "ISBN_1").save()
        new Book(name: "It's not about my bike", genre: Book.Genre.BIOGRAPHY, isbn: "ISBN_2").save()
        new Book(name: "Rules of the Game", genre: Book.Genre.NONFICTION, isbn: "ISBN_3").save()
    }


    void registerCustomJSONMarshallers() {
        JSON.createNamedConfig(JSONConstants.CUSTOM_JSON_MARSHALLER_GROUP){
            it.registerObjectMarshaller(new CustomDomainClassJSONMarshaller(false, grailsApplication), 2)
        }
        
        JSON.createNamedConfig(JSONConstants.SUMMARY_JSON_MARSHALLER_GROUP){
            it.registerObjectMarshaller(new CustomDomainClassJSONMarshaller(false, grailsApplication, JSONConstants.SUMMARY_JSON_MARSHALLER_GROUP), 2)
        }
    }
}
