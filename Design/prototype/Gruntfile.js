module.exports = function(grunt) {
  'use strict';

  // Load all the Grunt tasks listed in package.json
  require('matchdep').filterDev('grunt-*').forEach( grunt.loadNpmTasks );

  // ===========================================================================
  grunt.initConfig({

    haml: {
      all: {
        files: [{expand: true, cwd: 'src/', src: ['**/*.haml'], dest: 'src/', ext: '.html', flatten: false }],
      },
    },

    coffee: {
      all: {
        files: [{expand: true, cwd: 'src/', src: ['**/*.coffee'], dest: 'src/', ext: '-coffee.js', flatten: false }],
      },
    },

    watch: {
      options: {
        spawn: false,
      },
      haml:   { files: ['src/**/*.haml'],   tasks: ['haml'] },
      coffee: { files: ['src/**/*.coffee'], tasks: ['coffee'] },
    },

  });

  // ===========================================================================
  grunt.registerTask('default', ['haml', 'coffee']);

};

// vim:sw=2 ts=2 et:
