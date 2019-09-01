# Spring-MongoDB-Starter

This repository rappresent a starter project handled with Spring Boot and MongoDB.
Inside the project there are the following features:

1. A initial project tree to guide the developers to handling correctly a Spring Boot project, definition of constans and the use of tests with the resources folder..
2. Use of JPA repositories:
    - Use of extend of MongoRepository for the creation of the repositories.
    - Implementation of custom repository with use of interfaces.
3. Use of DTO (Data Transfer Object) to obtain data from the database.
4. Use of tests with <b>@SpringBootTest</b> and <b>@RunWith</b> annotations for testining the implementations of query and the logic put inside the servicies.
5. Use of versioning in database structure with the following features:
    - Restoring of dump files.
    - Versioning of modifications inside the database with version and subversion.
    - Restoring of modifications in base of version and subversion.
6. Use of logging with log4j (with logging of database queries).
7. Use of <b>@EnableConfigurationProperties</b> to build classes from application.properties file.
8. Use of <b>@Autowiring</b> inside no-Spring classes.

<h1>ApplicationContextConfiguration Class</h1>
The <i>ApplicationContextConfiguration</i> class allow the developers to use Spring components (Service, Repository, Component) inside No-Spring classes
with the use of the static method <i>getBean</i> that return the instance of the class past like argument.

<h1>StarterConfiguration Class</h1>
The <i>StarterConfiguration</i> class guide the developers to implement a configuration class that can be shared between the classes to read (or write) configuration informations taken from the application.properties file.

<h1>VersioningHandler</h1>
The <i>VersioningHandler</i> class rappresent with the principal funtionality class inside the project; it allow to use the migration operations. The class convert the MongoDB queries (with the shell syntax) inserted inside the following directories present in resources folder:
1. <b>dump</b>: This folder contains the dump files that must have the name of the collection on which the restore must be made.
2. <b>migration</b>: This folder contains the migration folders that must have a like name a: "<i>vX.Y</i>" where: <i>v</i> is the prefix, <i>X</i> is the version that must be an integer and <i>Y</i> is the subversion that must be an integer. Inside the folder we can put different files that must have a name like: "<i>vX.Y_Z</i>" where: <i>v</i> is the prefix, <i>X</i> is the version that must be an integer, <i>Y</i> is the subversion that must be an integer and <i>Z</i> is a (integer) incremental number that rappresent the version number of the file.

The queries supported by the class follow:
1. <b>createCollection</b>.
2. <b>createIndex</b>.
3. <b>createIndexes</b>.
4. <b>deleteOne</b>.
5. <b>deleteMany</b>.
6. <b>drop</b>.
7. <b>dropIndex</b>.
8. <b>dropIndexes</b>.
9. <b>insertOne</b>.
10. <b>insertMany</b>.
11. <b>remove</b>.
12. <b>renameCollection</b>.
13. <b>save</b>.
14. <b>update</b>.
15. <b>updateOne</b>.
16. <b>updateMany</b>.

Because the class use the syntax of MongoDB shell to traslate the queries in a language that Spring can be understend, please refer to <a href="https://docs.mongodb.com/manual/reference/method/js-collection/">mongodb references</a> 
for writing the migration files.
