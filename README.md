# A Solar-Powered IoT System For South African Farmers
It is hard for a lot of farmers in South Africa to keep up with the rapidly changing technology of the modern times. They constantly face issues such as load-shedding and an internet connection that might not be consistently high-speed. It is hard for them to accurately track and monitor their solar panels and other IoT devices, such as incubators.\
This is where our software SolarFlare comes in. SolarFlare is an application has been developed to help farmers and potential employees, linking their devices to solar panels and other IoT devices that might be used in their farms.

## SolarFlare
SolarFlare is a secure, database-driven mobile application blueprint with an offline-resilient, three-tier software system for the solar-powered IoT network. Farmers will be able to do the following:
- Connect to and monitor their solar panels, which involves checking statistics such as battery and energy consumed/saved.
- Write down notes regarding the solar panels, helping with tracking and reminders.
- Assign accounts to users with the ability to adjust their roles.

And they will be able to do all of that and more with ease, thanks to a UI that is simple and easy to understand.

## Tech Stack
The application is built with a highly robust architecture, which is explored more below.\
Front-end: Kotlin (Android). Kotlin allows the user to have a modern, fast, fluid, and reliable interface.\
Back-end: Java. Java is a stable and secure language, trusted by global banks and tech giants.\
Database: SQLite. SQLite is a highly structured database system that ensures that data is organised and can be scaled efficiently, being able to be retrieved instantly when a user needs it. SQLite also allows the database to be used even during offline access.\
API: FastAPI. FastAPI is a modern and fast web framework coded with Python.\
Authenticator: HMAC. Hash-Based Message Authentication Codes helps verify data integrity and authenticity by using a single shared secret key that is only known to the sender and receiver.\
Communication: The mobile devices communicate with the local edge gateway by being connected to the same WiFi. In situations where WiFi is not available, Bluetooth Low Energy (BLE) is used.

## How To Install And Use The Application
_(Note: This will be entered at a later stage in development)_

## System Architecture
This is the system architecture for the application. It delves into the details of how the application works, from the IoT device to the local gateway (which is the farm computer), from the database and API, and finally to the Android mobile devices. It also explains in detail what every step is.
<img width="3375" height="1519" alt="SolarFlare System Architecture" src="https://github.com/user-attachments/assets/d365b882-2492-434f-b798-4518564bab8d" />

## Screenshots
These are some screenshots of what the application looks like on a device, including the dashboard and information about the possible IoT devices.\
_(Note: Images are not final at the moment and are subject to change as development continues)_

<img width="377" height="666" alt="Login Screen" src="https://github.com/user-attachments/assets/17c31244-708b-455b-a55c-7f5e1f078a6e" />\
This is the page where users can sign up, with an option to log in if they already have an account.

<img width="374" height="799" alt="SolarFlare Profile Page" src="https://github.com/user-attachments/assets/3dbb85bc-ae01-4974-93d4-4547ce8b51b5" /> \
This is the Account page for logged in users, with numerous options to customise and personalise the user's experience. There is also a navigation bar at the bottom, helping users jump from page to page.

