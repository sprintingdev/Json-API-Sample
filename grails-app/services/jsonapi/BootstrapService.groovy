package jsonapi

import com.intelligrape.example.json.User
import com.intelligrape.example.json.Role
import com.intelligrape.example.json.UserRole
import com.intelligrape.example.json.Book
import grails.converters.JSON
import com.intelligrape.example.json.CustomDomainClassJSONMarshaller

class BootstrapService {

    static transactional = true

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
        new Book(name: "Tale of Two Cities", genre: Book.Genre.FICTION).save()
        new Book(name: "It's not about my bike", genre: Book.Genre.BIOGRAPHY).save()
        new Book(name: "Rules of the Game", genre: Book.Genre.NONFICTION).save()
    }


    void registerCustomJSONMarshallers() {
//        JSON.registerObjectMarshaller(new CustomJSONMarshaller(), 1)
        JSON.registerObjectMarshaller(new CustomDomainClassJSONMarshaller(false), 2)
    }
}
