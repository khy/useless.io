#!/usr/bin/env ruby
# == Wrapper script to update a local postgrseql database
#
# == Usage
#  ./dev.rb
#

Dir.chdir(File.dirname($0)) {
  command = "sem-apply --url postgres://localhost:5432/useless_workouts_test"
  puts command
  exec(command)
}
