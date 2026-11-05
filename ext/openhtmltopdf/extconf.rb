# frozen_string_literal: true

require "fileutils"

gem_root = File.expand_path("../..", __dir__)
java_dir = File.join(gem_root, "java")
jar_name = "openhtmltopdf-runner-0.1.0-jar-with-dependencies.jar"
out_dir  = File.join(gem_root, "lib", "openhtmltopdf", "java")

mvn = ENV.fetch("MVN", "mvn")
ok = system(mvn, "-B", "-q", "clean", "package", chdir: java_dir)

# A failed build never fails the install (it would stop a deploy); the JAR committed with the gem is used instead
if ok
  FileUtils.mkdir_p(out_dir)
  FileUtils.cp(File.join(java_dir, "target", jar_name), out_dir)
elsif File.exist?(File.join(out_dir, jar_name))
  warn "openhtmltopdf: Maven build failed, so the committed JAR is used. Install Java 11+ and Maven to rebuild it."
else
  warn "openhtmltopdf: Maven build failed and there is no JAR, so rendering will fail. Install Java 11+ and Maven."
end

# Nothing to compile or install beyond the JAR copied above.
File.write("Makefile", "all:\n\t@true\ninstall:\n\t@true\nclean:\n\t@true\n")
