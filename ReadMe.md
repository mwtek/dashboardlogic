# Dashboard Logic library for the UKB Dashboard Processor project

This project is a Dashboard Logic library project for the [UKB Dashboard Processor Project](https://www.github.com/mwtek/dashboarddataprocessor).

##Usage
This Library contains the algorithms to construct the json content that is supposed to be submitted
to the Dashboard. It relies on the "utilities" library. You cannot run this library on its own.
This is merely a part of the "dashboarddataprocessor" project and needs to be installed according to its
installation guide. The parameterizable constants of the logic can be found in `enums/Corona*.java` file.

## License
This project is released under the terms of the [GPL version 3](LICENSE.md).

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```


## Requirements
<a id="requirements"></a>
The code is platform independent and was tested on Linux and Windows environments.

The following software must be available to execute the program:

- Java 11 (JDK mandatory for compilation, OpenJDK recommended)
- Apache Maven (mandatory for compilation)

It is recommended to have both programs installed in a way that the correct versions of *java* and *mvn* are found in the system path.

In addition, at least 4 GB of free RAM, preferably 8 GB, should be available. The size depends on the number of resources that have to be retrieved from the FHIR server. In a test with ~ 500,000 resources, 4 GB was the lower limit. 
