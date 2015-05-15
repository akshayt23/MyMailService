
# MyMailService
Java based application that fetches data from a database and sends it out to an SMTP server.

###### Project Requirements : 
    * JavaMailApi
    * MySQL
    * SMTP server, I’m using fakeSMTP server running locally

###### Instructions to Run :
    * Install MySQL
    * Create a database called mails_db, the tables creation and insert logic is part of the code itself
    * Copy both JARs (fakeSMTP and MyMailService) to a folder and cd to that directory
    * Start fakeSMTP server by running 
        "java -jar fakeSMTP-2.0.jar -s -p 1123 -o output/"
    * Start MyMailService by running
        "java -jar MyMailService.jar"

###### Design :
    * The app fetches mails from a database and sends the out to an SMTP server.
    * Multiple threads run in parallel and fetch mails from the database in batches.
    * Batch size is calculated at runtime depending upon the number of threads, the number of mails and the maximum number of mails that can brought into memory (assumed as 10000).
    * Initially all mails are marked as unsent, once they are brought into memory to be sent out, they are marked as sent. Any mails that could not be sent are then marked as failed.
    * Each thread repeatedly brings mails from the database into the memory in batches and sends it out. This process is repeated unless no more unsent mails are left.
    * Another alternative to marking mails was to store the ids of in-flight and failed emails in separate tables.

