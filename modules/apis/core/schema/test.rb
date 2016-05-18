#!/usr/bin/env ruby
# == Wrapper script to update a local postgrseql database
#
# == Usage
#  ./test.rb
#

Dir.chdir(File.dirname($0)) {
  command = "sem-apply --url postgres://localhost/useless_core_test"
  puts command
  exec(command)
}
