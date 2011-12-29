package com.intelligrape.example.json

class Book {

    String name
    Genre genre
    String isbn

    static jsonProperties = [summary:['name']]

    static constraints = {
    }

    enum Genre{
        FICTION("Fiction"),
        NONFICTION("Non Fiction"),
        BIOGRAPHY("Biography")

        final String displayName;

        Genre(String displayName){
            this.displayName = displayName
        }

    }
}
