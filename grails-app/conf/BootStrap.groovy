class BootStrap {

    def bootstrapService

    def init = { servletContext ->
        bootstrapService.createRoles()
        bootstrapService.createDefaultUser()
        bootstrapService.createDefaultBooks()
        bootstrapService.registerCustomJSONMarshallers()
    }
    def destroy = {
    }
}
