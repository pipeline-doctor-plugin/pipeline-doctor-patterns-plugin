# Pipeline Doctor Patterns Plugin

[![Build Status](https://github.com/pipeline-doctor-plugin/pipeline-doctor-patterns-plugin/workflows/Build%20and%20Publish/badge.svg)](https://github.com/pipeline-doctor-plugin/pipeline-doctor-patterns-plugin/actions)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.md)

A pattern-based diagnostic provider for Jenkins Pipeline Doctor that analyzes build logs using predefined regex patterns to identify common build failures and provide actionable solutions.

## Overview

The Pipeline Doctor Patterns Plugin extends the [Pipeline Doctor Core Plugin](https://github.com/pipeline-doctor-plugin/pipeline-doctor-core-plugin) by providing a flexible pattern-matching system for diagnosing build failures. It uses YAML-based pattern definitions to identify issues in build logs and suggest solutions.

## Features

- **Pattern-Based Analysis**: Uses regex patterns to identify common build failures
- **YAML Configuration**: Easy-to-maintain pattern definitions
- **Extensible**: Add custom patterns for your specific use cases
- **High-Performance**: Efficient pattern matching with configurable timeouts
- **Capture Groups**: Extract relevant information from log messages
- **Confidence Scoring**: Each pattern has a confidence level for match accuracy

## Installation

### Prerequisites

- Jenkins 2.440.3 or newer
- Java 11 or newer
- [Pipeline Doctor Core Plugin](https://github.com/pipeline-doctor-plugin/pipeline-doctor-core-plugin) installed

### Installing from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/pipeline-doctor-plugin/pipeline-doctor-patterns-plugin.git
   cd pipeline-doctor-patterns-plugin
   ```

2. Build the plugin:
   ```bash
   mvn clean install
   ```

3. Upload the generated `.hpi` file from `target/` to Jenkins:
   - Navigate to **Manage Jenkins** → **Manage Plugins** → **Advanced**
   - Upload `pipeline-doctor-patterns.hpi`
   - Restart Jenkins

## Usage

Once installed, the plugin automatically analyzes failed builds and provides diagnostic results based on pattern matches.

### Built-in Patterns

The plugin includes patterns for common CI/CD failures:

- **Docker Issues**: Authentication errors, daemon not running, image pull failures
- **Git/VCS Issues**: Authentication failures, network errors, branch issues
- **Maven/Gradle Issues**: Dependency resolution, compilation errors
- **Network Issues**: Timeouts, DNS failures, proxy problems
- **Permission Issues**: File access, insufficient privileges
- **Resource Issues**: Out of memory, disk space

### Pattern Configuration

Patterns are defined in YAML format. Here's an example:

```yaml
patterns:
  - id: docker-auth-error
    name: "Docker Authentication Error"
    category: "Docker"
    regex: "unauthorized: authentication required.*(?:for\\s+)?(?:https?://)?([^/\\s]+)"
    confidence: 95
    multiline: false
    captures:
      - registry_url
    solutions:
      - title: "Login to Docker Registry"
        description: "Authenticate with the Docker registry"
        commands:
          - "docker login ${registry_url}"
        documentation: "https://docs.docker.com/engine/reference/commandline/login/"
```

### Adding Custom Patterns

1. Create a YAML file in `src/main/resources/patterns/`:
   ```yaml
   patterns:
     - id: custom-error
       name: "My Custom Error"
       category: "Custom"
       regex: "ERROR: Custom pattern (\\d+)"
       confidence: 90
       captures:
         - error_code
       solutions:
         - title: "Fix Custom Error"
           description: "Solution for error code ${error_code}"
   ```

2. Rebuild and redeploy the plugin

## Development

### Project Structure

```
pipeline-doctor-patterns-plugin/
├── src/main/java/           # Java source code
│   └── io/jenkins/plugins/pipelinedoctor/patterns/
├── src/main/resources/      # Resources
│   └── patterns/           # Pattern YAML files
├── src/test/java/          # Test files
├── docs/                   # Documentation
└── pom.xml                # Maven configuration
```

### Building

```bash
# Build without tests
mvn clean install -DskipTests

# Build with tests
mvn clean install

# Run specific tests
mvn test -Dtest=PatternMatcherTest
```

### Testing Patterns

The plugin includes comprehensive tests for pattern matching:

```java
@Test
public void testCustomPattern() {
    Pattern pattern = new Pattern();
    pattern.setRegex("ERROR: (\\w+) failed");
    pattern.setCaptures(Arrays.asList("component"));
    
    PatternMatcher matcher = new PatternMatcher();
    PatternMatchResult result = matcher.match("ERROR: Database failed", pattern);
    
    assertTrue(result.isMatched());
    assertEquals("Database", result.getCapture("component"));
}
```

## Architecture

The plugin implements the `DiagnosticProvider` interface from Pipeline Doctor Core:

```
DiagnosticProvider (Core)
    └── PatternBasedDiagnosticProvider
            ├── PatternLoader (loads YAML patterns)
            ├── PatternMatcher (executes regex matching)
            └── Pattern (pattern definition model)
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-pattern`)
3. Add your patterns or code changes
4. Add tests for new functionality
5. Commit your changes (`git commit -m 'Add amazing pattern'`)
6. Push to the branch (`git push origin feature/amazing-pattern`)
7. Create a Pull Request

### Pattern Contribution Guidelines

When contributing patterns:
- Ensure regex patterns are well-tested
- Include meaningful capture groups
- Provide actionable solutions
- Add documentation links when available
- Set appropriate confidence levels (0-100)

## Configuration

The plugin can be configured via Jenkins system configuration:

- **Pattern directories**: Additional directories to load patterns from
- **Analysis timeout**: Maximum time for pattern analysis
- **Pattern priorities**: Configure which patterns run first

## Troubleshooting

### Patterns Not Loading

Check Jenkins logs for pattern loading errors:
```
grep "PatternLoader" $JENKINS_HOME/logs/jenkins.log
```

### Performance Issues

- Reduce pattern complexity
- Use non-greedy regex quantifiers
- Enable multiline mode only when necessary
- Configure appropriate timeouts

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/pipeline-doctor-plugin/pipeline-doctor-patterns-plugin/issues)
- **Documentation**: [Wiki](https://github.com/pipeline-doctor-plugin/pipeline-doctor-patterns-plugin/wiki)
- **Core Plugin**: [Pipeline Doctor Core](https://github.com/pipeline-doctor-plugin/pipeline-doctor-core-plugin)

## Acknowledgments

- Jenkins community for the plugin development framework
- Contributors to the pattern library
- Users providing feedback and pattern suggestions